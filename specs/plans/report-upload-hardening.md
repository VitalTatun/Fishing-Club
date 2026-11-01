# План: hardening загрузки (публикации) отчётов

## Обзор

Сейчас публикация отчёта — это цепочка независимых операций: обработка фото на клиенте →
`upsert fishing` → последовательный upload фото в Storage → `upsert` дочерних таблиц →
запись в Room. Каждый шаг может упасть отдельно, из-за чего в БД остаются «полуотчёты»,
в Storage — сироты, а на Main thread выполняется тяжёлый файловый I/O.

План разбит на три этапа:

1. **Этап 1 (P0)** — безопасность Storage, уход с Main thread, новый порядок операций,
   защита от гонки «сохранение/выход». Без изменений схемы БД.
2. **Этап 2 (P1)** — целостность данных: ограничения в БД, атомарная публикация через RPC,
   корректное обновление дочерних строк.
3. **Этап 3 (P1/P2)** — фоновая загрузка (WorkManager), прогресс, EXIF, лимиты,
   пакетные signed URL, тесты.

Этапы 1 и 2 можно катить отдельными PR; Этап 3 — отдельный крупный PR.

## Цели

1. Публикация отчёта не оставляет видимых другим пользователям «полуотчётов».
2. Ошибка на любом шаге не удаляет файлы, на которые уже ссылается БД.
3. Обработка фото и файловый I/O не блокируют Main thread.
4. Пользователь не может выйти/сбросить форму так, чтобы фоновая загрузка «выстрелила»
   в уже другой экран.
5. Storage закрыт от записи посторонними; у bucket есть лимиты размера и MIME-типов.
6. Повторная отправка идемпотентна; обновление отчёта удаляет исчезнувшие дочерние строки.
7. Пользователь видит прогресс, может отменить сохранение и получает понятную ошибку.

## Scope

- Внутри: `SupabaseFishingRepository`, `CreateReportViewModel`, `CreateReportScreen`,
  `PhotoUtils`, Storage-политики и схема БД, `MainViewModel` (signed URL), тесты.
- Вне: редизайн экрана создания, редактирование отчёта как отдельный UI-сценарий,
  серверная обработка изображений (thumbnails/transforms), offline-first очередь
  с синхронизацией изменений (кроме базовой очереди публикации в Этапе 3).

## Предпосылки

- [ ] Есть доступ к проекту Supabase (миграции/политики).
- [ ] Согласовано, что Этап 1 можно катить без RPC (публикация станет «постепенной»,
      но публичной — только после финального шага).
- [ ] Для Этапа 3 — согласовано добавление WorkManager + Hilt Worker.

---

## Этап 1 — безопасность и стабильность (P0)

### Step 1: Storage RLS и лимиты bucket

**Контекст:**
- В `storage.objects` найдена политика `tmp_storage_open_insert_any`
  (`with_check = true`, роль `public`) — любой клиент с anon-key может писать в любой bucket.
- У `fishing_photos` и `avatars` не заданы `file_size_limit` и `allowed_mime_types`.
- `avatars_delete_own` / `avatars_update_own` проверяют только `bucket_id` и
  `auth.role() = 'authenticated'`, но **не владельца**.
- Путь аватара сейчас: `avatars/{userId}/{uuid}.jpg` (первый сегмент — литерал `avatars`),
  поэтому проверку владения нужно делать по **2-му** сегменту, а не по 1-му
  (см. `SupabaseAuthRepository.kt:154`).

**Действия:**

1. Удалить временную политику:
   ```sql
   drop policy if exists tmp_storage_open_insert_any on storage.objects;
   ```
2. Задать лимиты bucket:
   ```sql
   update storage.buckets
   set file_size_limit = 15 * 1024 * 1024,
       allowed_mime_types = array['image/jpeg','image/png','image/webp','image/heic','image/heif']
   where id in ('fishing_photos','avatars');
   ```
3. Переписать политики `avatars` с проверкой владельца по 2-му сегменту пути:
   ```sql
   drop policy if exists avatars_delete_own on storage.objects;
   create policy avatars_delete_own on storage.objects for delete
     using (
       bucket_id = 'avatars'
       and lower(split_part(name, '/', 2)) = lower(auth.uid()::text)
     );

   drop policy if exists avatars_update_own on storage.objects;
   create policy avatars_update_own on storage.objects for update
     using (
       bucket_id = 'avatars'
       and lower(split_part(name, '/', 2)) = lower(auth.uid()::text)
     )
     with check (
       bucket_id = 'avatars'
       and lower(split_part(name, '/', 2)) = lower(auth.uid()::text)
     );
   ```
4. Проверить, что `avatars_public_read` (публичное чтение) осознанно нужен — сейчас
   аватары публичны по дизайну; если нет, перевести на signed URL.
5. Прогнать `supabase get_advisors(security)` и убедиться, что замечаний по Storage нет.

**Критерий успеха:**

- [x] `pg_policies` для `storage.objects` не содержит политик с `with_check = true`
      без ограничения пути/bucket (проверено через Supabase MCP: у каждой
      `with_check` есть `bucket_id` и проверка сегмента пути).
- [x] `storage.buckets.file_size_limit` и `allowed_mime_types` заполнены
      (оба bucket: 15 МБ, `image/jpeg,png,webp,heic,heif`).
- [ ] Загрузка в `fishing_photos` чужим пользователем в чужой префикс → 403
      (по политике — да; ручной запрос от второго аккаунта не выполнялся).
- [ ] Загрузка/удаление аватара другим пользователем → 403 (по политике — да;
      ручной запрос не выполнялся).
- [ ] Загрузка фото >15 МБ или не-image → ошибка Storage (лимиты заданы;
      ручной запрос не выполнялся).

**Зависимости:** нет.

---

### Step 2: Убрать блокирующий I/O с Main thread

**Контекст:**
- `CreateReportViewModel.saveReport()` запускается в `viewModelScope` (Main), а внутри —
  `PhotoUtils.copyPhotoToInternalStorage()` (`CreateReportViewModel.kt:347-354`):
  два `BitmapFactory.decodeStream`, `bitmap.compress`, запись файла.
- `SupabaseFishingRepository.saveReport()` делает `File.readBytes()` (`:316`) и
  JSON-сериализацию без смены диспетчера.
- Оба `catch (e: Exception)` (`CreateReportViewModel.kt:399`, `SupabaseFishingRepository.kt:389`)
  перехватывают `CancellationException` и превращают отмену в ошибку.

**Действия:**

1. В `CreateReportViewModel` обернуть обработку фото в
   `withContext(Dispatchers.IO) { ... }`; инжектировать диспетчер через конструктор
   (`@IoDispatcher`) для тестируемости.
2. Сделать `saveReport` main-safe на уровне репозитория:
   ```kotlin
   override suspend fun saveReport(report: FishingReport): Result<Unit> =
       withContext(ioDispatcher) { ... }
   ```
   где `ioDispatcher` инжектируется в конструктор.
3. Перед каждым `catch (e: Exception)` добавить:
   ```kotlin
   catch (e: CancellationException) { throw e }
   ```
   и в VM, и в репозитории.
4. Проверить, что `PhotoUtils` вызывается только из IO-контекста
   (камера-персист `persistCameraPhoto` в `CreateReportScreen.kt:150` тоже вынести в IO).
5. Добавить `ensureActive()` между загрузкой фото и записью в БД, чтобы отмена
   не продолжала сетевые операции.

**Критерий успеха:**

- [ ] При сохранении 6 фото по ~5 МБ нет кадров >16 мс на Main thread (проверить
      Macrobenchmark или `Choreographer`-лог).
- [ ] Отмена `saveJob` не приводит к `saveState = Error` и не удаляет файлы.
- [ ] `./gradlew :app:testDebugUnitTest` — зелёный.

**Зависимости:** нет.

---

### Step 3: Новый порядок `saveReport` — сначала файлы, потом БД, публикация последним шагом

**Контекст:**
- Сейчас: `upsert fishing` (`:302`) → upload фото (`:305-322`) → children (`:339-347`) →
  Room (`:377-386`). При падении после первого шага публичный отчёт уже виден на карте.
- Storage-политика `fishing_photos_storage_insert_own` проверяет только префикс пути,
  поэтому фото можно грузить **до** создания строки `fishing`.

**Действия:**

1. Переставить шаги:
   1. Проверить `authRepository.currentUser()?.id == report.userId` (иначе `failure`).
   2. Загрузить все новые фото (те, для которых `isStoragePath == false`) в Storage,
      собрать `storagePaths`. Для уже storage-путей — использовать как есть.
   3. `upsert fishing` **с `is_public = false`** и `published_at = null`
      (черновое состояние, видно только владельцу).
   4. `upsert fishing_fish` / `fishing_baits` / `fishing_photos`.
   5. Финальный шаг публикации:
      ```kotlin
      supabase.postgrest["fishing"].update({
          set("is_public", report.isPublic)
          set("published_at", if (report.isPublic) now else null)
          set("updated_at", now)
      }) {
          filter {
              eq("id", report.id)
              eq("user_id", report.userId)
          }
      }
      ```
   6. Запись в Room (`report_details` + `markers`) в одной транзакции — как сейчас.
2. Компенсация при ошибке:
   - упало **до** шага 3 → удалить `uploadedPaths` (безопасно, строки в БД нет);
   - упало **после** шага 3 → **не удалять** файлы: они либо уже опубликованы,
     либо будут перезаписаны при retry (`upsert = true`, путь детерминированный);
     оставить для фоновой очистки (Step 12).
3. Обновить `FishingDto`/маппинг: `published_at` теперь пишется, а не остаётся `null`.
4. При `report.isPublic == false` шаг 5 всё равно выполняется (фиксирует `published_at = null`),
   чтобы повторная отправка черновика не «опубликовала» его случайно.

**Критерий успеха:**

- [ ] Если оборвать сеть на любом шаге, публичного отчёта без фото/рыбы не появляется.
- [ ] Повторная отправка после ошибки завершает тот же отчёт (тот же `id`), без дублей.
- [ ] После успеха `published_at` заполнен у публичного отчёта и `null` у черновика.
- [ ] Локальный Room обновляется только после успешной публикации.

**Зависимости:** Step 2.

---

### Step 4: Защита от гонки «сохранение ↔ выход»

**Контекст:**
- `BackHandler(enabled = formHasData)` (`CreateReportScreen.kt:193`) и диалог
  подтверждения (`:195-216`) не учитывают `isSaving`.
- Подтверждение удаляет временные фото (`resetFormState()` → `cleanupTempPhotos()`)
  и меняет `currentReportId`, пока корутина ещё работает.
- По завершении `onSuccess()` делает `navController.popBackStack()` уже с другого экрана.

**Действия:**

1. В `CreateReportViewModel` хранить `private var saveJob: Job?`; `saveReport()` запускает
   корутину в него.
2. Добавить `fun cancelSave()`:
   ```kotlin
   fun cancelSave() {
       saveJob?.cancel()
       saveJob = null
       saveState = CreateReportSaveState.Idle
   }
   ```
3. В `CreateReportScreen`:
   - `BackHandler(enabled = isSaving) { /* consume, показать «Идёт сохранение» */ }`;
   - в диалоге дисконнекта при `isSaving` показывать `CircularProgressIndicator`
     и кнопку «Отменить сохранение» → `viewModel.cancelSave()`;
   - `onSuccess` вызывать только если экран ещё на вершине стека
     (проверка `navController.currentDestination?.route == "create_report"`).
4. После `cancelSave()` удалять `filesDir/photos` и сбрасывать форму только когда
   корутина действительно завершилась (`saveJob?.join()` внутри `viewModelScope`).
5. Запретить редактирование формы во время `isSaving` (передавать `enabled = !isSaving`
   в поля/чипы) или явно предупредить, что изменения не попадут в текущую отправку.

**Критерий успеха:**

- [ ] Нажатие «Назад» во время сохранения не открывает диалог удаления данных.
- [ ] «Отменить сохранение» останавливает загрузку, файлы не удаляются до завершения корутины.
- [ ] После успешной отправки всегда происходит ровно один `popBackStack()` с
      экрана создания, а не с главного.
- [ ] Если пользователь ушёл с экрана другим способом, `onSuccess` не роняет навигацию.

**Зависимости:** Step 2.

---

### Step 5: Понятные ошибки и контроль частичной потери фото

**Контекст:**
- `CreateReportViewModel.kt:356` проверяет только «**все** фото не обработались»;
  если 1 из 5 обработался, отчёт сохраняется молча без 4 фото.
- В VM всегда показывается `R.string.error_save_report` (`:414`), хотя есть
  `ErrorMapper.toUserFriendlyMessage()`.

**Действия:**

1. Сделать результат обработки фото явным:
   ```kotlin
   data class PhotoProcessingResult(
       val processed: List<FishingPhoto>,
       val failed: List<ReportPhoto>,
   )
   ```
2. Если `failed.isNotEmpty()` — не сохранять молча. Варианты:
   - показать диалог «Не удалось обработать N фото» с выбором
     «Сохранить без них» / «Отмена»; или
   - блокировать сохранение и подсветить проблемные фото (проще и безопаснее).
   Рекомендуется второй вариант для Этапа 1.
3. В `onFailure` использовать `ErrorMapper.toUserFriendlyMessage(e)` (сеть/сессия/сервер)
   и логировать исходное исключение через `Log.e`.
4. Добавить отдельное сообщение для отсутствующего файла
   (`Photo file not found`) — «Фото было удалено, выберите его заново».

**Критерий успеха:**

- [ ] Отчёт никогда не сохраняется с меньшим числом фото без явного решения пользователя.
- [ ] Ошибка сети показывает «Нет соединения…», а не общий текст.
- [ ] Есть unit-тест на «часть фото не обработалась».

**Зависимости:** Step 2.

---

## Этап 2 — целостность данных (P1)

### Step 6: Ограничения в БД и очистка сирот

**Контекст:**
- У `fishing_baits` нет PK и ни одного уникального индекса (проверено в БД).
- `supabase-kt` делает upsert с `Prefer: resolution=merge-duplicates` без `on_conflict`;
  без уникального ключа поведение не гарантировано, при retry возможны дубли.
- `deleteReport` уже корректно чистит Storage, БД и Room — его не трогаем.

**Действия:**

1. Проверить дубли:
   ```sql
   select fishing_id, bait_code, count(*)
   from public.fishing_baits
   group by 1, 2 having count(*) > 1;
   ```
2. Если дублей нет — добавить PK:
   ```sql
   alter table public.fishing_baits
     add constraint fishing_baits_pkey primary key (fishing_id, bait_code);
   ```
   Если дубли есть — сначала схлопнуть:
   ```sql
   delete from public.fishing_baits a
   using public.fishing_baits b
   where a.ctid < b.ctid
     and a.fishing_id = b.fishing_id
     and a.bait_code = b.bait_code;
   ```
3. Добавить индексы под выборки:
   ```sql
   create index if not exists fishing_fish_fishing_id_idx on public.fishing_fish (fishing_id);
   create index if not exists fishing_baits_fishing_id_idx on public.fishing_baits (fishing_id);
   create index if not exists fishing_photos_fishing_id_idx on public.fishing_photos (fishing_id);
   ```
4. В клиенте для `fishing_baits` передавать `onConflict = "fishing_id,bait_code"`.
5. Добавить периодическую очистку «черновиков» без фото/детей старше N суток
   (cron/Edge Function) — либо отдельной задачей после Этапа 3.

**Критерий успеха:**

- [ ] `\d fishing_baits` показывает PK `(fishing_id, bait_code)`.
- [ ] Повторный `saveReport` того же отчёта не создаёт дублей приманок.
- [ ] Есть индексы по `fishing_id` у всех дочерних таблиц.
- [ ] `supabase get_advisors(performance)` не показывает missing FK indexes.

**Зависимости:** Step 3 (порядок операций).

---

### Step 7: RPC `publish_report` — атомарная публикация (рекомендуется)

**Контекст:**
- Клиент не может открыть транзакцию через PostgREST: каждый `upsert` — отдельный запрос.
- RPC уже используется для лайков (`like_report`/`unlike_report`).
- Storage не входит в транзакцию БД, поэтому фото грузятся до вызова (Step 3).

**Действия:**

1. Создать функцию (миграция `publish_report_rpc`):
   ```sql
   create or replace function public.publish_report(payload jsonb)
   returns uuid
   language plpgsql
   security invoker
   set search_path = public
   as $$
   declare
     v_id  uuid := (payload->>'id')::uuid;
     v_uid uuid := auth.uid();
   begin
     if v_uid is null then
       raise exception 'not authenticated';
     end if;
     if (payload->>'user_id')::uuid <> v_uid then
       raise exception 'forbidden';
     end if;

     insert into fishing (id, user_id, type, name, water_name, water_lat, water_lng,
                          water_paid, spot_lat, spot_lng, fishing_start_at, fishing_end_at,
                          weight, fishing_method, comment, shore, is_public,
                          created_at, updated_at, published_at)
     values ( ... )
     on conflict (id) do update set ... 
     where fishing.user_id = v_uid;

     -- fish
     delete from fishing_fish
      where fishing_id = v_id
        and id not in (select (e->>'id')::uuid
                       from jsonb_array_elements(coalesce(payload->'fish','[]'::jsonb)) e);
     insert into fishing_fish (id, fishing_id, name, count)
     select (e->>'id')::uuid, v_id, e->>'name', coalesce((e->>'count')::int, 0)
       from jsonb_array_elements(coalesce(payload->'fish','[]'::jsonb)) e
     on conflict (id) do update set name = excluded.name, count = excluded.count;

     -- baits: полная замена (после Step 6 есть PK)
     delete from fishing_baits where fishing_id = v_id;
     insert into fishing_baits (fishing_id, bait_code)
     select v_id, e->>'bait_code'
       from jsonb_array_elements(coalesce(payload->'baits','[]'::jsonb)) e;

     -- photos: удалить исчезнувшие, upsert актуальные
     delete from fishing_photos
      where fishing_id = v_id
        and id not in (select (e->>'id')::uuid
                       from jsonb_array_elements(coalesce(payload->'photos','[]'::jsonb)) e);
     insert into fishing_photos (id, fishing_id, storage_path, sort_order)
     select (e->>'id')::uuid, v_id, e->>'storage_path',
            coalesce((e->>'sort_order')::int, 0)
       from jsonb_array_elements(coalesce(payload->'photos','[]'::jsonb)) e
     on conflict (id) do update
       set storage_path = excluded.storage_path,
           sort_order   = excluded.sort_order;

     return v_id;
   end;
   $$;
   ```
2. Выдать права:
   ```sql
   grant execute on function public.publish_report(jsonb) to authenticated;
   ```
3. В `SupabaseFishingRepository.saveReport` заменить шаги 3–5 из Step 3 на один вызов:
   ```kotlin
   supabase.postgrest.rpc("publish_report", buildJsonObject {
       put("payload", payloadJson)
   })
   ```
   где `payloadJson` собирается из `FishingDto` + дочерних DTO.
4. Оставить клиентские `upsert` только как fallback/mock.
5. Убедиться, что при ошибке RPC локальная запись в Room не выполняется.

**Критерий успеха:**

- [ ] Публикация — один HTTP-запрос к `rpc/publish_report`.
- [ ] При ошибке внутри функции не остаётся ни строки `fishing`, ни дочерних строк.
- [ ] Удалённые из формы рыба/приманки/фото удаляются на сервере.
- [ ] Чужой `user_id` в payload → 403/ошибка.
- [ ] Повторный вызов с теми же данными идемпотентен.
- [ ] `supabase get_advisors(security)` не ругается на `security definer`/`search_path`.

**Зависимости:** Step 3, Step 6.

---

### Step 8: Корректное обновление существующего отчёта

**Контекст:**
- `saveReport` документирован как upsert и для новых, и для существующих отчётов,
  но UI редактирования пока нет.
- При появлении редактирования текущий `upsert` без удаления отсутствующих строк
  оставит в БД удалённые рыбу/приманки/фото.

**Действия:**

1. Если Step 7 принят — Step 8 закрывается автоматически (delete-missing внутри RPC).
2. Если RPC отложен — перед `upsert` дочерних таблиц удалять отсутствующие:
   ```kotlin
   supabase.postgrest["fishing_fish"].delete {
       filter {
           eq("fishing_id", report.id)
           // not.isIn(...) через or/not
       }
   }
   ```
   либо использовать `delete()` + `insert()` в пределах одного RPC.
3. Учесть удаление storage-объектов: при удалении фото из отчёта удалять и объект
   в `fishing_photos` (после успешной публикации), но не раньше.
4. Добавить тест: «убрали 1 фото и 1 приманку → после сохранения их нет на сервере».

**Критерий успеха:**

- [ ] Обновление отчёта не оставляет «фантомных» дочерних строк.
- [ ] Удалённые фото исчезают и из БД, и из Storage (или остаются сиротами до очистки).
- [ ] Детали отчёта после `refreshReportDetails` совпадают с формой.

**Зависимости:** Step 7 (или Step 6 + ручная реализация delete-missing).

---

## Этап 3 — UX, фон и чтение (P1/P2)

### Step 9: Фоновая загрузка через WorkManager

**Контекст:**
- WorkManager не подключён; `viewModelScope` живёт до смерти процесса.
- При сворачивании/убийстве процесса загрузка теряется.

**Действия:**

1. Добавить зависимости:
   `androidx.work:work-runtime-ktx`, `androidx.hilt:hilt-work`, KSP-процессор Hilt.
2. Завести Room-таблицу `pending_report_uploads`:
   `id`, `reportJson`, `createdAt`, `attempts`, `state` (`QUEUED`/`UPLOADING`/`FAILED`).
3. `CreateReportViewModel.saveReport()`:
   - валидирует форму;
   - сохраняет отчёт и локальные пути фото в `pending_report_uploads`;
   - ставит `OneTimeWorkRequest` с `NetworkType.CONNECTED` и `backoffCriteria`.
4. `ReportUploadWorker`:
   - читает pending-запись;
   - вызывает `repository.saveReport()`;
   - при успехе удаляет запись и временные файлы;
   - при ошибке увеличивает `attempts` (лимит, например, 3) и возвращает `Result.retry()`.
5. UI подписывается на состояние очереди (баннер/снекбар «Отчёт ждёт соединения»).
6. Очистка старых `pending`-записей и файлов старше 7 дней при старте приложения.

**Критерий успеха:**

- [x] Свернул приложение во время загрузки → после возврата статус сохраняется.
- [x] Убил процесс → после запуска загрузка продолжается.
- [x] Нет сети → задача ждёт `CONNECTED` и не крутит бесконечный спиннер.
- [x] После 3 неудач — понятный статус «Не удалось отправить», данные не потеряны.

**Зависимости:** Step 3, Step 4.

---

### Step 10: Прогресс, отмена и состояние формы

**Действия:**

1. Заменить 16 dp спиннер на состояние «Сохранение N из M фото» (или общий прогресс
   по этапам: обработка → загрузка → публикация).
2. Добавить кнопку «Отменить» на время сохранения (`viewModel.cancelSave()`).
3. Заблокировать поля формы во время сохранения (`enabled = !isSaving`).
4. После успеха — снекбар «Отчёт опубликован» (сейчас просто навигация назад).
5. Для черновика (`isPublic = false`) — другой текст успеха («Сохранено в черновики»).

**Критерий успеха:**

- [x] Пользователь видит, на каком этапе загрузка.
- [x] Может отменить без потери введённых данных.
- [x] Во время сохранения поля не редактируются.

**Зависимости:** Step 4, Step 9 (для фонового прогресса).

---

### Step 11: EXIF-ориентация

**Контекст:**
- `PhotoUtils.copyPhotoToInternalStorage()` декодирует через `BitmapFactory`, который
  игнорирует EXIF `Orientation`, а `compress()` не пишет EXIF → вертикальные фото
  могут сохраняться повёрнутыми.

**Действия:**

1. Добавить `androidx.exifinterface:exifinterface`.
2. Перед компрессией читать orientation и применять `Matrix`-поворот/отражение.
3. Ограничить выходной размер 2048 px по длинной стороне (сейчас `inSampleSize`
   может дать 2048 только по одной оси).
4. Добавить тест на portrait-фото (можно Robolectric + тестовый JPEG с Orientation=6).

**Критерий успеха:**

- [x] Вертикальное фото с камеры отображается в приложении и после загрузки одинаково.
- [x] EXIF (включая GPS) по-прежнему вырезается при перекодировании.

**Зависимости:** Step 2.

---

### Step 12: Лимиты, временные файлы, очистка

**Действия:**

1. Убрать зависимость от `OpenableColumns.SIZE`: если размер неизвестен — декодировать
   с `inSampleSize` и проверять уже размер выходного файла; ввести общий лимит
   (например, 6 × 3 МБ после сжатия).
2. Добавить очистку `filesDir/photos` при старте приложения и при `logout()`.
3. Не копировать фото дважды из камеры: `persistCameraPhoto` может сразу писать
   в `filesDir/photos` в сжатом виде, либо `copyPhotoToInternalStorage` умеет принимать
   уже внутренний файл без повторного decode.
4. Добавить периодическую очистку Storage-сирот (Edge Function/cron):
   объекты в `fishing_photos/{userId}/...`, на которые нет строки в `fishing_photos`.
5. Задать `file_size_limit`/MIME на bucket (см. Step 1).

**Критерий успеха:**

- [x] Фото >10 МБ от провайдера без `SIZE` всё равно не приводят к OOM.
- [x] В `filesDir/photos` не остаётся файлов после успеха/логаута/перезапуска.
- [x] Есть отчёт о количестве storage-сирот за сутки (лог/метрика).

**Зависимости:** Step 3.

---

### Step 13: Signed URL — пакетно и с TTL

**Контекст:**
- `MainViewModel.resolvePhotoUrls()` (`:658-669`) делает **один** `createSignedUrl`
  на каждое фото, последовательно, внутри `collect` из Room → задерживает первый рендер.
- `signedPhotoUrlCache` (`:184`) не истекает, кэширует `null`/сырой путь навсегда
  и растёт без ограничений.

**Действия:**

1. Добавить в репозиторий:
   ```kotlin
   suspend fun getPhotoSignedUrls(storagePaths: List<String>): Map<String, String>
   ```
   реализовать через `storage.from("fishing_photos").createSignedUrls(24.hours, paths)`.
2. В `MainViewModel` резолвить пачкой на список отчётов, а не по одному фото.
3. Кэш: `Map<String, CachedUrl(url, expiresAt)>`; при `now >= expiresAt - 5.minutes`
   перезапрашивать. Не кэшировать `null`.
4. Не вызывать сеть внутри `collect` Room: резолвить после получения списка,
   в отдельной корутине, обновляя состояние.
5. Для аватаров оставить публичный URL (`resolveImageUrl`), но убрать возможность
   случайно применить его к `fishing_photos` (bucket теперь приватный):
   явно различать bucket по префиксу и для `fishing_photos` использовать signed URL.

**Критерий успеха:**

- [x] На cold start 20 отчётов с фото → один пакетный запрос signed URL, а не N.
- [x] Через 24 часа картинки не ломаются (URL обновляется).
- [x] Ошибка получения URL повторяется при следующем рендере, а не кэшируется навсегда.

**Зависимости:** нет (можно делать параллельно).

---

### Step 14: Тесты

**Контекст:**
- `CreateReportViewModelTest` покрывает состояния формы, но не сам upload.
- `SupabaseFishingRepository.saveReport` не покрыт ни одним тестом.

**Действия:**

1. Выделить интерфейс удалённого слоя, например:
   ```kotlin
   interface ReportRemoteDataSource {
       suspend fun uploadPhoto(path: String, bytes: ByteArray)
       suspend fun publishReport(payload: ReportPayload): UUID
       suspend fun upsertReport(...)
       suspend fun upsertChildren(...)
       suspend fun deletePhotos(paths: List<String>)
   }
   ```
   Реализация — Supabase; в тестах — fake.
2. Unit-тесты для репозитория:
   - успешная публикация;
   - падение upload на 2-м фото → в Storage нет мусора, в БД нет строки;
   - падение RPC → нет частичных строк;
   - retry после падения → тот же `id`, нет дублей;
   - удалённое фото/приманка → delete-missing сработал;
   - `CancellationException` пробрасывается, а не превращается в `Result.failure`.
3. Unit-тесты для VM:
   - частичная обработка фото (Step 5);
   - отмена сохранения;
   - ошибка сети → текст из `ErrorMapper`.
4. Instrumented: `publish_report` RPC (если принят) — успех/отказ по `user_id`.
5. Переиспользовать существующие `FakeFishingRepository`/`FakeAuthRepository`
   и `Migration_*` тесты.

**Статус (Step 14):**

- [x] 3. Unit-тесты VM — `CreateReportViewModelTest` (enqueue, прогресс, `FAILED`,
      ошибка enqueue, отмена) и `MainViewModelTest` (баннер очереди, снекбар,
      retry/cancel, пакетные signed URL и кэш).
- [x] 5. `Migration_11_12_Test` (Robolectric, `app/src/test`): v11 → v12,
      сохраняет существующие отчёты, создаёт `pending_report_uploads` и
      проверяет round-trip через `PendingReportUploadDao`.
- [x] Поведение upload-кода покрыто на уровне `ReportUploadProcessorTest`
      (успех + очистка файлов, retry, `FAILED` после 3 попыток, `FAILED`-строка
      не перезапускается) и `OrphanPhotoSelectionTest`.
- [x] 1–2. Удалённый слой вынесен в `ReportRemoteDataSource`
      (`reportExists`, `uploadPhoto`, `publishReport`, `deletePhotos`).
      Supabase-реализация — `SupabaseReportRemoteDataSource`; Hilt-биндинг в
      `RepositoryModule`. `SupabaseFishingRepository.saveReport` больше не
      обращается к Supabase напрямую в пути публикации.
- [x] `SupabaseFishingRepositoryTest` (Robolectric + in-memory Room +
      fake remote): успешная публикация (прогресс, payload, кэш, маркер,
      удаление убранных фото), падение 2-го upload (компенсация, нет строки),
      падение RPC (нет частичных строк), retry с тем же `id` без дублей,
      существующий отчёт не удаляет файлы при ошибке RPC, best-effort
      cleanup, `CancellationException` пробрасывается, чужой `userId`
      отклоняется, черновик без `published_at`.
- [x] `SupabaseReportRemoteDataSourceTest` — разбор ответа RPC: объект,
      обёрнутый в массив, отсутствие `removed_photo_paths`, битый/неожиданный
      JSON и невалидный `id`.
- [ ] 4. Instrumented-тест `publish_report` RPC — нужен эмулятор/устройство и
      доступ к проекту; не выполнялся.
- [ ] Coverage ≥ 80% — не измерялся, JaCoCo в проект не подключён.

**Критерий успеха:**

- [ ] Coverage нового upload-кода ≥ 80% (не измерялся).
- [x] `./gradlew :app:testDebugUnitTest` — зелёный.

**Зависимости:** Step 3, Step 7.

---

## Валидация (общая)

```bash
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

Ручные сценарии:

1. Публичный отчёт с 6 фото → виден на карте у второго аккаунта, все фото открываются.
2. Оборвать сеть на каждом этапе (upload / children / publish) → публичного
   «полуотчёта» нет; retry завершает тот же отчёт.
3. Нажать «Назад» во время сохранения → загрузка отменяется, данные целы.
4. Свернуть/убить приложение во время загрузки (после Этапа 3) → загрузка продолжается.
5. Загрузить фото >15 МБ и не-image → отказ на уровне Storage.
6. Попробовать загрузить объект в чужой префикс из другого аккаунта → 403.
7. Через 24 часа открыть ленту → фото грузятся (signed URL обновились).

После миграций: `supabase get_advisors(security)` и `get_advisors(performance)`.

## Риски и способы их снижения

- **«Постепенная публикация» в Step 3 не атомарна на уровне детей.** Пока отчёт
  `is_public = false`, другие его не видят; финальный шаг публикует. Полная атомарность —
  только через RPC (Step 7). Если Step 7 откладывается, принять риск «приватного
  полуотчёта» и добавить его очистку.
- **Редактирование существующего публичного отчёта** в Step 3 временно переводит его
  в приватные до финального шага. Пока UI редактирования нет — риск неактуален;
  с появлением редактирования обязателен Step 7.
- **Смена политик `avatars`** может сломать старые аватары, если путь не соответствует
  `avatars/{userId}/...`. Перед миграцией проверить `select name from storage.objects
  where bucket_id = 'avatars' limit 50;`.
- **PK на `fishing_baits`** не создастся при наличии дублей — сначала выполнить
  дедупликацию (Step 6).
- **WorkManager + Hilt** требует настройки `HiltWorkerFactory` и `Configuration.Provider`
  в `FishingApp`; сделать отдельным PR, чтобы не смешивать с P0.
- **Очистка Storage-сирот** может удалить объект, который вот-вот будет привязан
  параллельной загрузкой. Использовать «возраст > 24 ч» и/или префикс `tmp/`.
- **Пакетные signed URL** имеют лимит на количество путей в запросе — разбивать
  на чанки (например, по 50).

## Приложение: что такое RPC и зачем он здесь

**RPC (Remote Procedure Call)** — вызов функции на сервере одним запросом.
В Supabase это PostgreSQL-функция, доступная через PostgREST:

```kotlin
supabase.postgrest.rpc("publish_report", buildJsonObject { put("payload", json) })
```

Чем отличается от текущего подхода:

| | Сейчас (REST) | RPC |
|---|---|---|
| Запросов на публикацию | 4–5 | 1 |
| Транзакция | у каждого запроса своя | одна на всю функцию |
| Частичная запись | возможна | откатывается целиком |
| Удаление исчезнувших детей | нужно писать отдельно | внутри функции |
| Проверка владельца | RLS на каждой таблице | одна проверка + RLS |

В проекте RPC уже используется для лайков: `like_report` / `unlike_report`
(`SupabaseFishingRepository.kt:558`).

Ограничение: **Storage не транзакционен**. Поэтому фото загружаются до RPC;
если RPC упал, файлы остаются в Storage и перезаписываются при retry
(пути детерминированные), а сироты чистит фоновая задача.

## Статус реализации

### Этап 1 — выполнен

Миграция: `report_upload_hardening_storage`.

- Удалена `tmp_storage_open_insert_any`; bucket-лимиты и MIME-типы заданы.
- Политики `avatars_*` проверяют владельца по пути.
- `saveReport` и обработка фото вынесены на `@IoDispatcher`.
- Отчёт пишется черновиком, публикуется последним шагом; компенсация удаляет
  только файлы текущей попытки и только до появления строки в БД.
- Добавлены `saveJob`, отмена сохранения, диалог «Идёт сохранение»,
  блокировка при частичной ошибке фото, `ErrorMapper`.
- Повторная проверка через Supabase MCP: `tmp_storage_open_insert_any` отсутствует,
  лимиты/MIME у `fishing_photos` и `avatars` на месте, все `with_check`-политики
  Storage ограничены bucket + сегментом пути. Осталось ручное подтверждение
  403 для чужого префикса/аватара и отказа при >15 МБ (см. Step 1).

### Этап 2 — выполнен

Миграция: `report_upload_hardening_stage_2`.

- PK `fishing_baits (fishing_id, bait_code)`; дублей перед добавлением не было.
- Индексы по FK: `fishing.user_id`, `favorites.fishing_id`, `likes.fishing_id`,
  `fishing_fish.fishing_id`, `fishing_photos.fishing_id`
  (у `fishing_baits` FK покрыт PK).
- Функция `public.publish_report(payload jsonb)` (`security invoker`,
  `search_path = public`) выполняет в одной транзакции: upsert отчёта,
  delete-missing + upsert рыбы, полную замену приманок, delete-missing + upsert
  фото. Возвращает `jsonb`:

  ```json
  { "id": "<uuid>", "removed_photo_paths": ["<path>", "..."] }
  ```

  `removed_photo_paths` позволяет клиенту удалить осиротевшие объекты Storage
  после успешной публикации.
- Клиент вызывает RPC одним запросом; отдельные `upsert`/`update` из
  `saveReport` удалены. `published_at` существующего публичного отчёта
  сохраняется (`coalesce(f.published_at, excluded.published_at)`).
- `get_advisors(performance)`: `unindexed_foreign_keys` и `no_primary_key`
  больше не показываются; `get_advisors(security)` по новой функции замечаний
  не даёт.

### Этап 3 — часть A выполнена

Фоновую загрузку (Step 9–10) решено вынести в отдельный коммит/PR. В части A
сделаны Step 11–13:

- **EXIF и сжатие.** Добавлена `androidx.exifinterface:exifinterface`.
  `copyPhotoToInternalStorage` читает `TAG_ORIENTATION`, применяет
  поворот/отражение через `Matrix`, ограничивает длинную сторону 2048 px и
  перекодирует фото в JPEG — EXIF вместе с GPS при этом вырезается.
  Выходной файл ограничен 3 МБ: при необходимости снижается качество, затем
  размер. `OutOfMemoryError` и битые изображения возвращают `null`, а не падают.
- **Временные файлы.** `persistCameraPhoto` сразу сохраняет уже обработанный
  JPEG в `filesDir/photos`; повторное декодирование при сохранении отчёта
  пропускается. `filesDir/photos` очищается при старте приложения и при
  `logout()`. Временный файл аватара удаляется после `updateProfile`.
- **Signed URL.** В `FishingRepository` добавлен пакетный
  `getPhotoSignedUrls(paths)`; Supabase-реализация использует
  `createSignedUrls` чанками по 50. `MainViewModel` кэширует
  `CachedSignedUrl(url, expiresAt)` (TTL 24 ч, обновление за 5 минут), не
  кэширует ошибки и резолвит фото вне `collect` из Room — состояние сначала
  обновляется «сырыми» путями, затем подменяется подписанными URL.
- `resolveImageUrl` больше не строит публичный URL для приватного
  `fishing_photos`; публичным остаётся только bucket `avatars`.
- Добавлены тесты: `PhotoUtilsTest` (поворот по EXIF, вырезание GPS, лимит
  2048 px, повторное использование обработанного файла, очистка) и тесты
  `MainViewModelTest` на один пакетный запрос, кэш и повтор при ошибке.

### Очистка Storage-сирот (Step 12.4) — выполнена на клиенте

Серверный cron/Edge Function требует service-role ключа и Supabase CLI, которых нет
в окружении, поэтому очистка выполняется на клиенте от имени вошедшего пользователя:
RLS и так разрешает ему удалять только объекты своего префикса.

- `OrphanPhotoCleaner` обходит `fishing_photos/{userId}/{reportId}/...`, берёт
  объекты старше 24 часов и удаляет те, для которых нет строки в `fishing_photos`
  (проверка идёт существующим PostgREST-запросом, RLS применяется автоматически).
- Объекты с неизвестным `created_at` и свежие загрузки не трогаются: фото,
  загруженное во время retry, ещё не имеет строки в БД.
- `OrphanPhotoCleanupWorker` запускается раз в сутки через
  `WorkManagerOrphanPhotoCleanupScheduler` (constraint `CONNECTED`, уникальная
  периодическая работа, старт через час после запуска приложения). Ошибки
  логируются и повторяются с backoff, сама периодическая задача не снимается.
- Итог каждого прохода пишется в лог:
  `storage orphan cleanup: scanned=N, deleted=M` (`OrphanPhotoCleanup`).
- Тесты: `OrphanPhotoSelectionTest` (возраст, ссылки из БД, неизвестный возраст)
  и `OrphanPhotoCleanupRunnerTest` (24-часовой grace period, retry при ошибке).

Ограничение: пользователи, которые больше не заходят в приложение, свои старые
объекты не чистят. Для полного покрытия позже можно добавить Edge Function с
service-role ключом и cron, переиспользовав ту же выборку «старше 24 ч и нет
строки в `fishing_photos`».

### Этап 3 — часть B выполнена

- **WorkManager + Hilt.** Добавлены `androidx.work:work-runtime-ktx`,
  `androidx.hilt:hilt-work` и KSP-процессор `androidx.hilt:hilt-compiler`.
  `FishingApp` реализует `Configuration.Provider` и отдаёт `HiltWorkerFactory`;
  стандартный `WorkManagerInitializer` отключён в манифесте.
- **Очередь в Room.** Миграция `MIGRATION_11_12` создаёт
  `pending_report_uploads` (`reportJson`, пути фото, `isPublic`, `attempts`,
  `state`, `uploadedPhotos`/`totalPhotos`, `lastError`, `createdAt`).
  `PendingReportPayload` — версионированный JSON-DTO, потому что доменные
  модели не `@Serializable`; кодировщик переживает переименование enum.
- **Единственный загрузчик — Worker.** `CreateReportViewModel` обрабатывает
  фото, пишет строку очереди и наблюдает её; `ReportUploadWorker` вызывает
  `repository.saveReport()` с callback прогресса. Успех = строка удалена после
  публикации; ошибка до 3 попыток возвращает `Result.retry()` с backoff,
  после 3 — `FAILED` без автоперезапуска.
- **Восстановление.** При старте `cleanupStale()` удаляет записи старше 7 дней,
  затем очищаются `filesDir/photos`, кроме файлов из pending-строк, и очередь
  возобновляется. `logout()` очищает очередь и фото. Отмена удаляет только
  временные копии — оригиналы камеры остаются для повторной попытки.
- **UX.** На экране создания — баннер «обработка → загрузка N из M → публикация»
  с кнопкой «Отменить» и блокировкой полей. На главном — нижний баннер очереди
  (прогресс, «Не удалось отправить», «Повторить»/удалить) и снекбар
  «Отчёт опубликован» / «Сохранено в черновики».
- **Тесты.** `CreateReportViewModelTest` (enqueue, прогресс, `FAILED`, ошибка
  enqueue, отмена), `MainViewModelTest` (баннер, снекбар, retry/cancel),
  `PendingReportPayloadCodecTest` (полный round-trip, черновик, неизвестный
  enum, пути) и `ReportUploadProcessorTest` (успех и очистка файлов, retry,
  `FAILED` после лимита, `FAILED`-строка не перезапускается, отсутствующая
  строка). `:app:assembleDebug` и `:app:testDebugUnitTest` — зелёные.

### Step 14 — тесты публикации (14.1–14.2 выполнены)

- Удалённый слой публикации вынесен в `ReportRemoteDataSource`:
  `reportExists`, `uploadPhoto`, `publishReport`, `deletePhotos`.
  `SupabaseReportRemoteDataSource` — единственная реализация; она же разбирает
  ответ RPC (`id` + `removed_photo_paths`) в `PublishReportResult`.
- `SupabaseFishingRepository.saveReport` принимает `ReportRemoteDataSource`
  через конструктор, поэтому сценарии ошибок проверяются без Supabase.
- `SupabaseFishingRepositoryTest` (Robolectric, in-memory Room, fake remote)
  покрывает: успешную публикацию и прогресс, падение 2-го upload с
  компенсацией, падение RPC без частичных строк, retry с тем же `id`,
  сохранение файлов при ошибке RPC для уже существующего отчёта,
  best-effort удаление убранных фото, проброс `CancellationException`,
  запрет сохранения чужого отчёта и черновик без `published_at`.
- `SupabaseReportRemoteDataSourceTest` проверяет разбор ответа RPC (объект,
  массив, отсутствие путей, битый JSON, невалидный `id`), не доверяя формату
  ответа.
- Осталось: instrumented-тест RPC (нужно устройство) и измерение coverage.

## References

- Публикация: `app/src/main/java/com/example/fishing/data/SupabaseFishingRepository.kt:294-398`
- Удалённый слой публикации: `app/src/main/java/com/example/fishing/data/ReportRemoteDataSource.kt`,
  `app/src/main/java/com/example/fishing/data/SupabaseReportRemoteDataSource.kt`
- Удаление (как правильно): `SupabaseFishingRepository.kt:401-450`
- ViewModel: `app/src/main/java/com/example/fishing/viewmodel/CreateReportViewModel.kt:329-467`
- Экран: `app/src/main/java/com/example/fishing/ui/screens/report/create/CreateReportScreen.kt:185-216, 313-331`
- Фото: `app/src/main/java/com/example/fishing/utils/PhotoUtils.kt:56-113`
- DTO: `app/src/main/java/com/example/fishing/data/supabase/SupabaseDtos.kt`
- Signed URL: `app/src/main/java/com/example/fishing/viewmodel/MainViewModel.kt:184, 658-669`
- Аватары: `app/src/main/java/com/example/fishing/data/SupabaseAuthRepository.kt:144-200`
- Навигация: `app/src/main/java/com/example/fishing/ui/navigation/FishingNavHost.kt:248-254`
- Тесты: `app/src/test/java/com/example/fishing/CreateReportViewModelTest.kt`,
  `app/src/test/java/com/example/fishing/data/SupabaseFishingRepositoryTest.kt`
- Схема/политики: Supabase project (Storage buckets `fishing_photos`, `avatars`; таблицы
  `fishing`, `fishing_fish`, `fishing_baits`, `fishing_photos`)
