# План: Избранные отчеты (Favorites)

## Обзор

Пользователь может отмечать отчеты как избранные (свои и чужие публичные) и видеть их
в общем списке вкладки «Главная». Избранное хранится в Supabase (таблица `favorites`)
и кэшируется локально в Room отдельной таблицей, чтобы общий refresh «моих отчетов»
(`deleteAll()`) не затирал избранное. У избранных чужих отчетов подтягивается имя автора из `profiles`.

## Цели

1. Хранить избранное на бэкенде: таблица `favorites` + RLS.
2. Кэшировать избранные отчеты (включая чужие/публичные) в отдельной Room-таблице.
3. Показывать избранные в общем списке «Главная» (свои + избранные, перемешаны, сортировка по fishingTime).
4. Toggle-кнопки (bookmark) в списке и на экране деталей.
5. Подтягивать автора (имя) избранного чужого отчета из `profiles`.

## Scope

- Внутри: бэкенд-таблица, репозиторий (интерфейс + Supabase + Mock), Room, ViewModel, UI (список + детали).
- Вне: отдельный экран «лента чужих отчетов», фильтр «только избранные».

## Предпосылки

- [ ] Пользователь авторизован (favorites привязаны к `auth.uid()`).

## Шаги

### Step 1: Бэкенд — таблица `favorites` + RLS + политика профиля

Создает таблицу «избранное» и позволяет читать чужие профили (для показа автора).

**Контекст:**
- Текущие RLS-политики: `fishing_select` уже допускает `is_public = true`; `profiles_select_own` разрешает читать только свой профиль.

**Действия:**
1. Создать таблицу через `supabase_apply_migration` (`create_favorites_table`):
   - `user_id uuid NOT NULL REFERENCES profiles(id) ON DELETE CASCADE`
   - `fishing_id uuid NOT NULL REFERENCES fishing(id) ON DELETE CASCADE`
   - `created_at timestamptz NOT NULL DEFAULT now()`
   - `PRIMARY KEY (user_id, fishing_id)`
   - `ALTER TABLE ... ENABLE ROW LEVEL SECURITY`
2. Политики RLS:
   - `INSERT` (WITH CHECK `user_id = auth.uid()`)
   - `SELECT` / `DELETE` (USING `user_id = auth.uid()`)
3. Добавить SELECT-политику на `profiles` для чтения чужих строк (например, `auth.role() = 'authenticated'`), чтобы подтягивать имя автора избранного.

**Критерий успеха:**
- [ ] `public.favorites` перечислена в `list_tables` (verbose) с PK `(user_id, fishing_id)` и `rls_enabled=true`.
- [ ] Политики присутствуют в `pg_policies` для всех трех команд.
- [ ] `SELECT` на `profiles` разрешает чужие строки.
- [ ] Пробная вставка/выборка/удаление работает; чужие favorites недоступны.

**Зависимости:** нет.

### Step 2: Room — отдельная таблица избранного + DAO

**Контекст:**
- `AppDatabase.kt` (version = 3, fallbackToDestructiveMigration)
- `ReportDetailsEntity.kt`, `ReportDetailsDao.kt`, `Converters.kt`, `DatabaseModule.kt`

**Действия:**
1. Создать `FavoriteReportEntity` по образцу `ReportDetailsEntity` (id PK, userId, water*, type, name, spots, fishingTime, weight, fishingMethod, comment, shore, isPublic, imageUrls, fishJson, baitsJson) + доп. поля `authorName: String?`, `authorAvatar: String?`.
2. Создать `FavoriteReportDao`: `getAll(): Flow<List<...>>`, `insertAll`, `insert`, `deleteById(fishingId)`, `deleteAll`.
3. Зарегистрировать entity и DAO в `AppDatabase.kt`; поднять `version` до 4 (destructive migration приемлема).
4. Добавить `@Provides` для DAO в `DatabaseModule.kt`.

**Successы:**
- [ ] Компилируется `app` (AppDatabase version 4).
- [ ] Room-таблица favorites авто-создается без crash.
- [ ] DAO покрывает: поток, вставка списка, вставка одной, удаление по id, полная очистка.

**Зависимости:** Step 1 (схема для маппинга).

### Step 3: Репозиторий — DTO, интерфейс, Supabase и Mock

**Контекст:**
- `FishingRepository.kt` (`MockFishingRepository` внутри)
- `SupabaseFishingRepository.kt`
- `SupabaseDtos.kt`

**Действия:**
1. В `FishingRepository` добавить:
   - `fun getFavoriteReports(userId: UUID? = null): Flow<List<FishingReport>>`
   - `suspend fun refreshFavorites(userId: UUID)`
   - `suspend fun addFavorite(report: FishingReport)`
   - `suspend fun removeFavorite(reportId: UUID)`
   - Реализации-mock в `MockFishingRepository` (пустой список / локальный список).
2. В `SupabaseDtos.kt` добавить `FavoriteDto(fishing_id: UUID, created_at: String? = null)`.
3. В `SupabaseFishingRepository.kt`:
   - `addFavorite`: `INSERT INTO favorites (user_id, fishing_id)` + upsert `FavoriteReportEntity` в Room (authorName из `report.user.name`).
   - `removeFavorite`: `DELETE FROM favorites` + `favoriteDao.deleteById`.
   - `getFavoriteReports`: `favoriteDao.getAll().map { it.toDomain() }` где `user` строится из `authorName`.
   - `refreshFavorites`: select из `favorites` где `user_id = current`; для каждого fishing_id подтянуть `fishing`+`fishing_fish`+`fishing_baits`+`fishing_photos` (как в `refreshReportDetails`), + `profiles` для авторов; переписать Room (deleteAll + insertAll).
   - Собственный маппинг `FavoriteReportEntity.toDomain()` (не наследовать баг `authRepository.currentUser()` в user).
4. Проверить, что `refreshAllReports` (own) не трогает favorites (отдельная таблица).

**Successы:**
- [ ] Интерфейс и обе реализации компилируются.
- [ ] add/remove пишут в Supabase (без ошибок RLS) и обновляют Room.
- [ ] После toggle и перезапуска `refreshFavorites` восстанавливает список избранного.

**Зависимости:** Step 1, Step 2.

### Step 4: ViewModel + UI (Home, детали, bookmark)

**Контекст:**
- `MainViewModel.kt`
- `MainScreen.kt`
- `FishingReportItem.kt` (bookmark в хедере, ~L114-118)
- `ReportDetailScreen.kt` (bookmark в TopAppBar, ~L49-55)
- `MainActivity.kt`

**Действия:**
1. `MainViewModel`: `favorites: StateFlow<Set<UUID>>` (или список), метод `toggleFavorite(report: FishingReport)`; вызывать `refreshFavorites()` в `refresh()`/`refreshAll()` и после toggle.
2. `MainScreen`:
   - Объединить `reports + favorites`: `distinctBy { id }`, сортировка по `fishingTime` desc.
   - Передавать `favoriteIds: Set<UUID>` и `onToggleFavorite: (FishingReport) -> Unit` в `FishingReportItem`.
3. `FishingReportItem`: bookmark в хедере сделать интерактивным (filled/outlined по `isFavorite`, `onClick = onToggleFavorite`).
4. `ReportDetailScreen`: рабочая bookmark-кнопка в TopAppBar (добавить `isFavorite` и `onToggleFavorite`).
5. `MainActivity`: пробросить `onToggleFavorite = { viewModel.toggleFavorite(it) }` в `MainScreen`.

**Successы:**
- [ ] Home показывает избранное (свои + чужие) в одной ленте.
- [ ] Bookmark в списке и в детали переключается и влияет на SQL-tin.
- [ ] Автор чужих отображается в `report.user` (name).
- [ ] После перезапуска/refresh избранное сохраняется.
- [ ] `./gradlew :app:assembleDebug` — без ошибок.

**Зависимости:** Step 3.

## Валидация (общая)

- `./gradlew :app:assembleDebug`
- `supabase_get_advisors` (security) после миграции.
- Ручной сценарий: отметить чужой публичный отчет с карты → проверить Home, перезапуск, снятие.

## Риски и способы их снижения

- **Room destructive migration** — кэш сбросится при обновлении схемы; приемлемо (уже настроено) для Home.
- **RLS favorites** — некорректный uuid/тип столбцов → insert/delete вернут 403; проверять логи `supabase_get_logs(service: postgres/api)`.
- **Показ автора** — если не хотите открывать профили, можно убрать `authorName` и показывать без имени.
- **Refresh-конфликт `deleteAll()` в refreshAllReports** — избранное изолировано отдельной таблицей.

## References

- Модель: `app/src/main/java/com/example/fishing/model/FishingReport.kt`
- Repo: `app/src/main/java/com/example/fishing/data/FishingRepository.kt`, `data/SupabaseFishingRepository.kt`, `data/supabase/SupabaseDtos.kt`
- Room: `data/local/AppDatabase.kt`, `entity/ReportDetailsEntity.kt`, `dao/ReportDetailsDao.kt`, `converter/Converters.kt`, `local/DatabaseModule.kt`
- ViewModel: `viewmodel/MainViewModel.kt`
- UI: `ui/screens/main/MainScreen.kt`, `ui/components/FishingReportItem.kt`, `ui/screens/report/detail/ReportDetailScreen.kt`, `MainActivity.kt`