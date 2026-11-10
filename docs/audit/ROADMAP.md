# Fishing — план работ по аудиту

**Дата аудита:** 2026-11-09  
**Статус:** активен  
**Текущий фокус:** `AUD-04` — авторитетное «отчёта нет» должно очищать приватный кэш
**Следующее действие:** при `Success` из Room и сетевом `null` удалять локальную строку и выставлять `Empty`; отделить сетевую ошибку от «отчёта нет». Перед ручной проверкой AUD-03 применить миграцию `supabase/migrations/20261110090000_account_deletion.sql`.

> Это handoff-файл аудита. Код правится по мере выполнения задач; актуальные статусы — в таблице ниже.

## Как продолжить в новой сессии

В начале новой сессии достаточно написать:

```text
@docs/audit/ROADMAP.md Продолжаем с AUD-03. Сначала прочитай файл и статусы.
```

Агент прочитает этот файл, увидит текущий фокус, порядок работ, решения и критерии готовности. После выполнения пункта нужно обновить его `Статус` и `Текущий фокус` здесь же.

## Принятые решения

1. **Release signing отложен до Release Gate.** Пока нет внешнего релиза, debug-подпись не блокирует разработку. Не распространять release-APK, собранный с debug-ключом. Перед первым APK для тестеров/сторов вернуться к `AUD-10`.
2. **Начинаем с P1**, а не с P0: сначала ANR и изоляция кэша маркеров.
3. **R8 не делаем первым.** Сначала стабилизируем логику и приватность, затем включаем R8 с проверкой release-сборки.
4. **Backup/auth-токены** остаются P0 по impact, но не блокируют старт P1. Закрыть до появления реальных пользовательских данных; можно делать параллельно.
5. **Приватность важнее косметики.** «Сделать приватным», удаление аккаунта и очистка приватного кэша — не откладывать дальше первых двух итераций.
6. **Выбор аватара вынесен в отдельную задачу `AUD-12`.** В AUD-03 не трогаем аватар: текущий пикер и диалог имени остаются как есть до реализации отдельного экрана.

## Очередь работ

| ID | Приоритет | Статус | Задача |
|---|---|---|---|
| AUD-01 | P1 | `DONE` | ANR при сохранении профиля |
| AUD-02 | P1 | `DONE` | Изоляция кэша маркеров по пользователю |
| AUD-03 | P1 | `DONE` | Рабочие «Сделать приватным» / смена пароля / удаление аккаунта |
| AUD-04 | P1 | `TODO` | Очистка приватного отчёта из кэша после скрытия на сервере |
| AUD-05 | P2 | `TODO` | R8, shrink resources, размер APK, baseline profile |
| AUD-06 | P2 | `TODO` | Сохранение состояния форм при повороте/process death |
| AUD-07 | P2 | `TODO` | Адаптивность и edge-to-edge/IME |
| AUD-08 | P2 | `TODO` | Nominatim: клиент, User-Agent, таймауты |
| AUD-09 | P2 | `TODO` | Окружения, baseline-миграции Supabase, CI |
| AUD-10 | Release Gate | `DEFERRED` | Release signing: keystore, Play App Signing, секреты |
| AUD-11 | P0 | `TODO` | Backup + plaintext Supabase-сессия + приватные данные |
| AUD-12 | P2 | `TODO` | Экран выбора аватара и сохранение без диалога имени |

## Детали задач

### AUD-01 — ANR при сохранении профиля

**Проблема:** `EditProfileViewModel.updateProfile()` работает в `viewModelScope` (`Dispatchers.Main`), внутри вызывает bitmap decode/scale/compress/file write и `File.readBytes()`.

**Где:**
- `app/src/main/java/com/example/fishing/viewmodel/EditProfileViewModel.kt:42-52`
- `app/src/main/java/com/example/fishing/utils/PhotoUtils.kt:89-130`
- `app/src/main/java/com/example/fishing/data/SupabaseAuthRepository.kt:169`

**Что сделать:** обернуть обработку фото и чтение файла в `withContext(ioDispatcher)`; прокинуть `IoDispatcher` в `EditProfileViewModel`; убедиться, что `readBytes()` не выполняется на main.

**Готово, когда:**
- сохранение большого фото не блокирует main thread;
- спиннер/прогресс отрисовывается во время обработки;
- `:app:testDebugUnitTest` зелёный;
- вручную проверено сохранение аватара на устройстве.

**Сделано (2026-11-09):**
- `EditProfileViewModel` получает `@IoDispatcher`; `PhotoUtils.copyPhotoToInternalStorage` вызывается внутри `withContext(ioDispatcher)`. Состояние `isLoading` выставляется до приостановки, поэтому спиннер рисуется во время обработки.
- Удаление временного файла после загрузки тоже вынесено в `withContext(ioDispatcher + NonCancellable)`, чтобы cleanup срабатывал и при отмене scope.
- `CancellationException` больше не проглатывается общим `catch (e: Exception)`.
- `SupabaseAuthRepository` получает `@IoDispatcher`; `file.readBytes()` читается на IO, а не на Main.
- Новый `app/src/test/java/com/example/fishing/EditProfileViewModelTest.kt`: 4 теста (обработка фото уходит в IO-диспетчер, сохранение на worker-thread с удалением temp-копии, нечитаемое фото не блокирует сохранение, ошибка репозитория показывает сообщение и гасит `isLoading`).
- `:app:testDebugUnitTest` — 267 тестов, 0 падений.

**Итог:** закрыто 2026-11-09. Ручная проверка на устройстве подтверждена владельцем: фото сохраняется.

### AUD-02 — Изоляция кэша маркеров

**Проблема:** `MarkerEntity` не содержит `userId`, `MarkerDao.getAll()` возвращает все маркеры; при смене аккаунта без явного logout возможен показ приватных маркеров предыдущего пользователя.

**Где:**
- `app/src/main/java/com/example/fishing/data/local/dao/MarkerDao.kt:13-15`
- `app/src/main/java/com/example/fishing/data/report/MarkerStore.kt:32-45`
- `app/src/main/java/com/example/fishing/viewmodel/MainViewModel.kt:199-213`
- `app/src/main/java/com/example/fishing/data/SupabaseAuthRepository.kt:131-144`

**Что сделать:** минимально — очищать marker cache при любом переходе в `Unauthenticated`; правильно — добавить `userId` в `MarkerEntity` и фильтровать запросы, плюс миграция Room.

**Готово, когда:**
- после `Unauthenticated` таблица `markers` пуста;
- вход под аккаунтом B не показывает приватные маркеры аккаунта A (публичные A B видеть обязан — это правило приложения);
- миграция проверена тестом;
- `:app:testDebugUnitTest` зелёный.

**Семантика `userId`:** это автор отчёта, а не зритель. Кэш маркеров общий для всех аккаунтов на устройстве, видимость решается по каждой строке: `isPublic = 1 OR userId = :viewerId` — то же правило, что на сервере и в RLS. Хранить зрителя нельзя: тогда публичные маркеры чужих авторов пропали бы у всех остальных.

**Сделано (2026-11-09, полный вариант):**
- `MarkerEntity` получил `userId` (автор) с KDoc; `MarkerDao.getAll()` заменён на `getVisibleForUser(viewerId)` — «все маркеры» больше нельзя прочитать даже случайно.
- `FishingDto.toMarkerEntity` проставляет автора из `dto.userId`; после публикации `ReportWriter` кладёт маркер с тем же автором.
- `RoomMarkerStore.getMapMarkers()` следует за `authState`: `flatMapLatest` переподписывается на строки текущего зрителя, поэтому чужие приватные маркеры исчезают сразу при смене аккаунта, ещё до сетевого refresh.
- `MainViewModel` при смене пользователя делает `map.clear()` перед загрузкой — иначе `loadMapMarkers(force = false)` рано выходил и B наследовал фильтры/камеру A.
- Миграция Room 12→13: таблица `markers` пересоздаётся с `userId BLOB NOT NULL`, legacy-строки удаляются (их автора восстановить нельзя; это одноразовая потеря офлайн-кэша, карта перезаливается сетью). Остальные кэши не затрагиваются.
- Минимальный шаг сохранён как вторая линия защиты: `MarkerCacheCleaner` очищает таблицу при `Unauthenticated` (logout, отозванный refresh-token, cold start без сессии).
- Тесты: `MarkerDaoTest` (видимость: B видит публичные A и свои приватные, приватные A не видит), `MarkerStoreTest` (A → Unauthenticated → B без refresh), `Migration_12_13_Test` (схема, NOT NULL, стирание legacy-кэша, DAO работает), обновлён `Migration_11_13_Test` и instrumented-цепочки `Migration_9_10`/`Migration_10_11` до v13.
- `:app:testDebugUnitTest` — 273 теста, 0 падений.

**Остаточный риск (вне AUD-02):** если отчёт был публичным при кэшировании, а потом стал приватным на сервере, маркер живёт до сетевого refresh. Это класс AUD-04 (авторитетное «отчёта нет» должно чистить кэш).

### AUD-03 — Приватность и аккаунт

**Проблема (было):** «Редактировать» и «Сделать приватным/публичным» только закрывали меню; «Изменить пароль» и «Удалить аккаунт» — no-op.

**Где:**
- `app/src/main/java/com/example/fishing/ui/screens/report/detail/ReportDetailScreen.kt:214-240`
- `app/src/main/java/com/example/fishing/ui/navigation/FishingNavHost.kt:561-562`
- `app/src/main/java/com/example/fishing/ui/screens/profile/EditProfileScreen.kt:70-79`

**Что сделать:** реализовать действия или скрыть/задизейблить их. Аватар в эту задачу не входит — он вынесен в `AUD-12`.

**Готово, когда:**
- смена приватности реально меняет `is_public` на сервере;
- удаление аккаунта и смена пароля работают;
- UI-тесты обновлены.

**Сделано (2026-11-10):**
- Пункт «Редактировать» удалён из меню отчёта вместе с неиспользуемой иконкой и строкой; функционала редактирования в приложении не будет.
- Приватность: перед изменением меню открывает `AlertDialog` с кратким описанием последствий («станет виден только вам» / «появится на карте уловов») и кнопками «Скрыть»/«Опубликовать». После подтверждения dialog сразу закрывается, запрос уходит, а результат (`Отчёт опубликован` / `Отчёт теперь виден только вам`) или ошибка показываются в snackbar. `ReportWriter.updateVisibility` меняет только `is_public`, `published_at` и `updated_at` в `fishing` (PostgREST update + `select()`). Пустой ответ RLS означает «нет прав/отчёта» и не зеркалится в кэш. После подтверждения сервером в одной Room-транзакции обновляются `report_details` и `markers`, поэтому бейдж «Не опубликован», домашняя лента и карта перерисовываются из источника истины. В меню есть состояние `isUpdatingVisibility`, блокирующее повторное действие.
- Смена пароля: отдельный маршрут `ChangePasswordRoute` и экран `ChangePasswordScreen`; `ChangePasswordViewModel` валидирует минимум 6 символов и совпадение паролей, вызывает `auth.updateUser { password = ... }`, показывает загрузку/ошибку и snackbar об успехе. Диалог с текстовыми полями не использовали: он вызывал зависание Compose в Robolectric.
- Удаление аккаунта: подтверждающий диалог в настройках профиля, `AuthRepository.deleteAccount()` вызывает RPC `public.delete_account()`, затем очищает локальные кэши отчётов/избранного/лайков/маркеров и очередь загрузки. Новая миграция `supabase/migrations/20261110090000_account_deletion.sql` удаляет отчёты, профиль и строку `auth.users` только для `auth.uid()`.
- Тесты: `ReportDetailScreenUiTest` (пункт редактирования отсутствует; приватность сначала показывает dialog с подтверждением; отмена ничего не отправляет; подтверждение шлёт противоположное состояние; успех показывается в snackbar; действие заблокировано во время запроса), `ReportDetailCoordinatorTest` (успех/ошибка смены видимости и результат), `MainViewModelTest` (проксирование, результат и обновление кэша), `EditProfileViewModelTest` (удаление аккаунта), `ChangePasswordViewModelTest`, `EditProfileScreenUiTest`, `ChangePasswordScreenUiTest`.
- `:app:testDebugUnitTest` — 298 тестов, 0 падений.

**Требуется перед ручной проверкой:** применить миграцию `20261110090000_account_deletion.sql` к Supabase-проекту; без неё удаление аккаунта вернёт ошибку «функция не найдена». Ручная проверка на устройстве не выполнялась.

### AUD-04 — Приватный отчёт остаётся в кэше

**Проблема:** при `Success` из Room сетевой `null` от сервера не очищает кэш; `ReportDetailCoordinator` выставляет `Empty` только из `Loading`.

**Где:**
- `app/src/main/java/com/example/fishing/viewmodel/detail/ReportDetailCoordinator.kt:83-87`
- `app/src/main/java/com/example/fishing/data/report/ReportDetailsStore.kt:42-49`

**Что сделать:** при авторитетном «отчёта нет» удалять локальную строку и выставлять `Empty`; сетевую ошибку обрабатывать отдельно, чтобы не терять офлайн-кэш.

**Готово, когда:**
- после снятия отчёта с публикации повторное открытие показывает «недоступен»;
- офлайн-открытие не удаляет кэш;
- тест на `Success -> null` есть.

### AUD-05 — R8 и размер APK

**Проблема:** `isMinifyEnabled = false`, `isShrinkResources` не задан, `proguard-rules.pro` пустой, APK ~63 МБ, 4 dex, ~16 МБ шрифтов.

**Где:**
- `app/build.gradle.kts:32-40`
- `app/build.gradle.kts:61`
- `app/src/main/java/com/example/fishing/ui/theme/Type.kt:12-21`
- `app/build/outputs/apk/release/app-release.apk`

**Что сделать:** включить оптимизацию R8, ресурсный shrink, заменить `material-icons-extended`, сабсетить шрифты, добавить baseline profile; проверить release-сборку на устройстве.

**Готово, когда:**
- release-сборка проходит и запускается;
- Hilt/Room/Serialization/Supabase не падают в release;
- размер APK существенно меньше;
- есть замер startup/scroll до и после.

### AUD-06 — Сохранение состояния форм

**Проблема:** `WaterEdit`, `CommentEdit`, `CatchEdit`, `MethodAndBait`, `EditProfile` используют `remember`; `CreateReportViewModel` не использует `SavedStateHandle`; путь к временному фото камеры тоже в `remember`.

**Где:**
- `app/src/main/java/com/example/fishing/ui/screens/report/create/WaterEditScreen.kt:60-62`
- `app/src/main/java/com/example/fishing/ui/screens/report/create/CommentEditScreen.kt:41`
- `app/src/main/java/com/example/fishing/ui/screens/report/create/CatchEditScreen.kt:32-38`
- `app/src/main/java/com/example/fishing/ui/screens/report/create/FishingMethodAndBaitScreen.kt:18-19`
- `app/src/main/java/com/example/fishing/ui/screens/profile/EditProfileScreen.kt:47-48`
- `app/src/main/java/com/example/fishing/ui/screens/report/create/CreateReportScreen.kt:141`

**Что сделать:** `rememberSaveable`/`SavedStateHandle` для черновиков, формы и temp-файла; при необходимости — сохранение черновика в Room/DataStore.

**Готово, когда:**
- поворот не теряет введённые данные;
- process death не теряет форму или восстанавливает черновик;
- результат камеры не пропадает после recreation.

### AUD-07 — Адаптивность и edge-to-edge

**Проблема:** фиксированный `NavigationBar`, одна колонка ленты, нет `WindowSizeClass`; поисковые экраны не учитывают navigation bar; login/registration без `imePadding`; FAB full-map без `navigationBarsPadding`.

**Где:**
- `app/src/main/java/com/example/fishing/ui/screens/main/MainShell.kt:207-229`
- `app/src/main/java/com/example/fishing/ui/screens/main/HomeTabContent.kt:133-141`
- `app/src/main/java/com/example/fishing/ui/screens/search/ReportSearchScreen.kt:181-204`
- `app/src/main/java/com/example/fishing/ui/screens/search/LocationSearchScreen.kt:75-104`
- `app/src/main/java/com/example/fishing/ui/screens/login/LoginScreen.kt:84-90`
- `app/src/main/java/com/example/fishing/ui/screens/map/MapScreen.kt:444-447`

**Что сделать:** `NavigationSuiteScaffold`/rail, адаптивные сетки, максимальная ширина контента, корректные insets и IME, проверка phone/tablet/foldable.

### AUD-08 — Nominatim

**Проблема:** новый `HttpClient` на экран, не закрывается; User-Agent маскируется под Chrome; нет таймаутов и внятного error state.

**Где:**
- `app/src/main/java/com/example/fishing/data/network/NominatimService.kt:15-25`
- `app/src/main/java/com/example/fishing/ui/screens/search/LocationSearchScreen.kt:40`

**Что сделать:** singleton-клиент через DI, честный User-Agent, таймауты, закрытие, состояние ошибки/повтора.

### AUD-09 — Окружения, миграции, CI

**Проблема:** `applicationId = com.example.fishing`, `minSdk = 36`, Supabase URL/key захардкожены, нет flavors; в репозитории только hardening-миграции Supabase; CI отсутствует.

**Где:**
- `app/build.gradle.kts:14-16`
- `app/src/main/java/com/example/fishing/data/SupabaseModule.kt:19-22`
- `docs/testing.md:56-64`
- `supabase/migrations/`

**Что сделать:** определить minSdk, заменить applicationId до публикации, вынести backend в `BuildConfig`/flavors, добавить baseline-миграции и CI-прогон unit + RLS-тестов.

### AUD-10 — Release signing (Release Gate)

**Решение:** пока не трогаем; вернуться перед первым APK для тестеров/сторов.

**Что проверить тогда:** release keystore из CI-секрета, Play App Signing, отсутствие паролей в VCS, воспроизводимая сборка, проверка сертификата через `apksigner`.

### AUD-11 — Backup и plaintext-сессия (P0, до реальных данных)

**Проблема:** `allowBackup=true`, пустые backup-правила; Supabase Auth по умолчанию хранит сессию в plain `SharedPreferences`; в Room/файлах лежат приватные отчёты, координаты, фото и очередь с e-mail.

**Где:**
- `app/src/main/AndroidManifest.xml:14-16`
- `app/src/main/res/xml/backup_rules.xml:8-12`
- `app/src/main/res/xml/data_extraction_rules.xml:6-12`
- `app/src/main/java/com/example/fishing/data/SupabaseModule.kt:43`

**Что сделать:** `allowBackup=false` или явные exclude; encrypted session manager; не бэкапить приватные кэши; убрать лишний e-mail из pending payload.

### AUD-12 — Экран выбора аватара

**Проблема:** в `EditProfileScreen` камера открывает системный пикер, но выбранный `Uri` только показывается в превью. Сохранение аватара возможно лишь как побочный эффект диалога изменения имени (`onSaveClick(name, selectedPhotoUri)`), а без диалога выбор теряется. Это обходной путь, а не отдельный сценарий.

**Где:**
- `app/src/main/java/com/example/fishing/ui/screens/profile/EditProfileScreen.kt:70-79`
- `app/src/main/java/com/example/fishing/ui/screens/profile/EditProfileScreen.kt:98-107`
- `app/src/main/java/com/example/fishing/ui/screens/profile/EditProfileScreen.kt:134-137`
- `app/src/main/java/com/example/fishing/viewmodel/EditProfileViewModel.kt:54-101`

**Что сделать:**
- отдельный экран/маршрут выбора аватара: превью, выбор из галереи, при необходимости камера, кнопки «Сохранить»/«Отмена»;
- загрузка через `EditProfileViewModel`/отдельный ViewModel без диалога имени: имя не должно быть обязательным условием сохранения фото;
- состояния загрузки, ошибки и успеха; очистка временного файла при отмене;
- `contentDescription` для кнопки камеры вместо английского «Change photo»;
- UI-тесты на выбор, сохранение, отмену и ошибку.

**Готово, когда:**
- аватар сохраняется без изменения имени и без диалога;
- отмена выбора не меняет профиль и не оставляет временных файлов;
- при ошибке загрузки профиль остаётся прежним, пользователь видит сообщение;
- UI-тесты обновлены, `:app:testDebugUnitTest` зелёный.

## Backlog / низкий приоритет

- `FileProvider` отдаёт весь cacheDir: `app/src/main/res/xml/file_paths.xml:3`.
- Захардкоженные строки и неполные переводы: `app/src/main/java/com/example/fishing/ui/screens/report/create/CreateReportComponents.kt:278`, `app/src/main/java/com/example/fishing/ui/screens/report/create/ReportFieldRenderer.kt:142`, `app/src/main/res/values/strings.xml:48-50`.
- Доступность: `contentDescription = null` у навигационных иконок, английский "Change photo": `app/src/main/java/com/example/fishing/ui/screens/profile/EditProfileScreen.kt:119`, `:213`.
- Устаревшие зависимости и старый lint-отчёт: `app/build/reports/lint-results-debug.txt`.
- Локальный секрет `.env.figma` — не коммитить; при утечке ротировать.
- Логи с `printStackTrace`/`e.message` — заменить на безопасный логгер.

## Проверки

```bash
./gradlew :app:testDebugUnitTest
```

Инструментальные тесты (при наличии устройства):

```bash
./gradlew :app:connectedDebugAndroidTest
```

Перед release-проверками:

```bash
./gradlew :app:assembleRelease
./gradlew :app:lintDebug
```

## Ограничения текущего аудита

- Динамический ANR-профайлинг не выполнялся; вывод по ANR основан на подтверждённом main-thread path.
- Свежий lint не запускался из-за ограничений песочницы; существующий отчёт устарел.
- RLS повторно не прогонялся; использованы существующий аудит и миграции.
- 298 unit/Compose-тестов проходят; 21 instrumented-тест проходил на устройстве 16 октября (после v13 миграции instrumented-тесты обновлены, но на устройстве не перезапускались).
- Ручная проверка сохранения аватара на устройстве выполнена владельцем: фото сохраняется. Сам сценарий выбора аватара остаётся прежним до `AUD-12`.
- Смена приватности, смена пароля и удаление аккаунта проверены unit/Compose-тестами, но не на живом Supabase. Для удаления аккаунта сначала нужно применить миграцию `20261110090000_account_deletion.sql`; объекты Storage при удалении аккаунта не чистятся (остаточный риск, см. AUD-09).
- Выбор аватара не входит в AUD-03 и вынесен в `AUD-12`.
