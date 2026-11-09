# Разделение MainViewModel по пользовательским сценариям

## 1. Исходное состояние

`app/src/main/java/com/example/fishing/viewmodel/MainViewModel.kt` — 993 строки,
45+ публичных членов. Один класс одновременно управляет:

| Сценарий | Состояния / методы | Примерно строк |
|---|---|---:|
| Лента отчётов | `reports`, `sortedReports`, `homeUiState`, `isInitialLoading`, `isRefreshing`, `isLoadingMore`, `loadMoreError`, `hasMoreReports`, `error`, сортировка, режим отображения, пагинация | 300 |
| Избранное | `favoriteIds`, `favoriteOpsInFlight`, `favoriteError`, `filterFavoriteReports`, оптимистичные оверлеи, загрузка списка избранного | 180 |
| Лайки | `likeStates`, `likeError`, оптимистичные оверлеи, гонки с Room-эмиссиями | 130 |
| Карта | `mapMarkers`, `mapIsLoading`, `mapIsRefreshing`, `mapRefreshError`, фильтры карты, запрошенная точка, полигон, камера | 120 |
| Поиск | `searchQuery`, дата и 5 фильтров в Compose `mutableStateOf` | 30 |
| Детали отчёта | `reportDetailUiState`, удаление, `deletedReportId`, ошибки | 100 |
| Signed URL фото | кэш, мьютекс, версии для ленты/избранного/деталей | 120 |
| Сессия | `currentUserId`, `loadedForUserId`, реакция на login/logout | 50 |
| Загрузка отчётов | очередь, баннер, уведомления | 40 |

### Проблемы

1. **Высокая связанность.** Любое изменение ленты затрагивает файл, в котором
   лежат карта, лайки, избранное и детали.
2. **Скрытые общие изменяемые поля.** `currentUserId`, оптимистичные оверлеи и
   версии фото живут в одном объекте; сценарии не изолированы.
3. **Дублирование бизнес-логики фильтрации.** `ReportSearchScreen` и `MapScreen`
   независимо реализуют фильтры «избранное / трофей / платный водоём / вид рыбы /
   способ ловли». Изменение правила нужно вносить в двух местах.
4. **Поиск и фильтры карты — Compose-состояние в ViewModel.** Их сложно тестировать
   как обычное состояние и они не следуют общему StateFlow-контракту экрана.
5. **Гонки при смене пользователя.** Проверки `currentUserId` размазаны по
   корутинам трёх сценариев.

## 2. Решение

`MainViewModel` остаётся **тонким фасадом** для существующего UI и сохраняет
публичный контракт (включая конструктор), поэтому `MainViewModelTest` и навигация
не переписываются. Ответственность выносится в сценарные координаторы состояния —
это вариант, явно допустимый постановкой наряду с отдельными ViewModel.

Почему координаторы, а не отдельные Hilt-ViewModel:

- экран `Main` уже получает один `MainViewModel` через `hiltViewModel()`;
  несколько ViewModel потребовали бы менять владельцев `ViewModelStore` и
  синхронизировать общие `favorites`/`likes`/`reports` между экранами;
- существующие тесты и публичный контракт `MainViewModelTest` должны остаться
  зелёными без переписывания;
- координаторы — обычные классы с явным `CoroutineScope`, их проще тестировать
  изолированно, чем Hilt-ViewModel;
- переход к отдельным ViewModel остаётся возможным позже: координатор уже
  является самостоятельной границей сценария.

### Целевая структура

```
viewmodel/
  MainViewModel.kt                    — фасад: связывает сценарии, auth, upload-уведомления
  MainUiStates.kt                     — HomeUiState, ReportDetailUiState (публичные типы)
  session/UserSession.kt              — текущий пользователь + безопасное разрешение id
  photos/PhotoUrlResolver.kt          — кэш signed URL, батч-запросы, версии не хранит
  home/HomeFeedCoordinator.kt         — лента, пагинация, сортировка, homeUiState
  favorites/FavoritesCoordinator.kt   — избранное, оптимистичные переключения, список избранного
  likes/LikesCoordinator.kt           — лайки, оптимистичные переключения
  map/MapCoordinator.kt               — маркеры, загрузка/refresh, фильтры карты, камера
  search/SearchCoordinator.kt         — состояние поиска (StateFlow)
  detail/ReportDetailCoordinator.kt   — детали отчёта, удаление
  filter/ReportFilters.kt             — единые предикаты фильтрации отчётов и маркеров
```

### Границы ответственности

- **MainViewModel** — композиция сценариев, реакция на `authState`, сброс
  пользовательских данных, очередь загрузки отчётов и уведомления, делегирование
  публичных методов. Не содержит бизнес-логики ленты/карты/поиска.
- **UserSession** — единственное место, где хранится `currentUserId`; даёт
  `resolve()` (с fallback на `authRepository.currentUser()`) и `bind()`.
- **PhotoUrlResolver** — только кэш и разрешение URL. Версии (`homePhotosVersion`,
  `favoritePhotosVersion`, `detailPhotosVersion`) остаются у сценариев, которым они
  принадлежат.
- **HomeFeedCoordinator** — `reports`, сортировка, `homeUiState`, пагинация,
  загрузка/refresh, режим отображения.
- **FavoritesCoordinator** — оптимистичное избранное, `favoriteIds`,
  `favoriteOpsInFlight`, ошибки, `filterFavoriteReports` и его загрузка.
- **LikesCoordinator** — оптимистичные лайки по тем же правилам, что и сейчас.
- **MapCoordinator** — маркеры, состояния загрузки, `ReportFilters` карты,
  запрошенная точка, подсвеченный полигон, сохранённая камера.
- **SearchCoordinator** — `ReportSearchState` в `StateFlow`; без знания о
  репозиториях.
- **ReportDetailCoordinator** — детали, `Empty`/`Error`, удаление и его ошибки.
- **ReportFilters** — единые чистые предикаты для `FishingReport` и `MarkerDomain`.

### Поток данных

```
AuthRepository.authState
        │
        ▼
   MainViewModel ──► UserSession
        │              │
        ├──► HomeFeedCoordinator ──► PhotoUrlResolver ──► FishingRepository
        ├──► FavoritesCoordinator ─► PhotoUrlResolver
        ├──► LikesCoordinator
        ├──► MapCoordinator
        ├──► SearchCoordinator
        └──► ReportDetailCoordinator ─► PhotoUrlResolver

UI ◄── StateFlow / события ── MainViewModel (делегирование)
```

### Совместимость UI

- Публичные `StateFlow` и методы `MainViewModel` сохраняются; `MainScreen`,
  `MainShell`, `HomeTabContent`, `WaterEditScreen` не меняются.
- `ReportSearchScreen` переходит с семи отдельных параметров на
  `ReportSearchState` + `onStateChange`; `FishingNavHost` передаёт состояние из
  `viewModel.searchState`.
- `MapScreen` получает `ReportFilters` из `viewModel.mapFilters` через
  `collectAsStateWithLifecycle`; локальный режим без ViewModel (preview/тесты)
  сохраняется.
- Фильтрация в обоих экранах использует общие `ReportFilters.matches(...)`.

## 3. Этапы

1. **Общие компоненты.** `UserSession`, `PhotoUrlResolver`, `ReportFilters` +
   `ReportSearchState`. Чистые предикаты покрыть unit-тестами.
2. **Лента.** `HomeFeedCoordinator`; facade делегирует `reports`, `sortedReports`,
   `homeUiState`, пагинацию и настройки сортировки/режима.
3. **Избранное и лайки.** `FavoritesCoordinator`, `LikesCoordinator`; сохранить
   оптимистичные оверлеи, guard от двойного тапа и проверки устаревших ответов.
4. **Карта.** `MapCoordinator`; фильтры и камера переезжают туда, UI получает
   `StateFlow<ReportFilters>`.
5. **Поиск.** `SearchCoordinator`; `ReportSearchScreen` переводится на
   `ReportSearchState`, дублирующиеся предикаты заменяются на `ReportFilters`.
6. **Детали и удаление.** `ReportDetailCoordinator`.
7. **Тонкий facade.** `MainViewModel` — только auth, upload, делегирование,
   `clearUserContent`.
8. **Тесты и проверка.** Целевые тесты координаторов и предикатов;
   `:app:testDebugUnitTest`, `:app:assembleDebug`.

## 4. Правила безопасности

- Логику не менять «по ходу»: переносить код и состояния дословно, включая
  `CancellationException` rethrow, порядок сброса и тексты ошибок.
- `MainViewModelTest` не редактировать под новую структуру.
- Не менять навигацию, экраны профиля, создания отчёта и загрузки.
- `StateFlow` создавать только через `MutableStateFlow` + `asStateFlow()`;
  производные состояния — `stateIn(scope, WhileSubscribed(5000), initial)`.
- После каждого этапа компилировать; финально — полный набор unit-тестов и
  `assembleDebug`.

## 5. Критерии готовности

- `MainViewModel.kt` ≤ ~300 строк, без бизнес-логики ленты/карты/поиска.
- Публичный контракт `MainViewModel` и `MainViewModelTest` не изменены.
- `ReportSearchScreen` и `MapScreen` используют общие предикаты фильтрации.
- Новые компоненты покрыты целевыми unit-тестами.
- `:app:testDebugUnitTest` и `:app:assembleDebug` зелёные.

## 6. Статус выполнения

Реализовано полностью.

| Компонент | Строк | Назначение |
|---|---:|---|
| `MainViewModel.kt` | 305 | тонкий фасад: auth, upload, делегирование |
| `home/HomeFeedCoordinator.kt` | 217 | лента, пагинация, сортировка, `homeUiState` |
| `favorites/FavoritesCoordinator.kt` | 190 | избранное и список избранного |
| `likes/LikesCoordinator.kt` | 171 | лайки |
| `detail/ReportDetailCoordinator.kt` | 142 | детали и удаление |
| `map/MapCoordinator.kt` | 137 | карта, фильтры, камера |
| `photos/PhotoUrlResolver.kt` | 153 | общий кэш signed URL |
| `filter/ReportFilters.kt` | 88 | общие предикаты поиска и карты |
| `search/SearchCoordinator.kt` | 29 | состояние поиска |
| `session/UserSession.kt` | 30 | единый источник `currentUserId` |
| `MainUiStates.kt` | 22 | публичные UI-состояния |

Итог:

- `MainViewModel.kt`: 993 → 305 строк; публичный контракт для экранов и
  `MainViewModelTest` сохранён.
- `ReportSearchScreen` и `MapScreen` используют единые `ReportFilters`;
  дублирование пяти фильтров удалено.
- Поиск и фильтры карты переведены на `StateFlow` + атомарные `update {}`
  события; `FishingNavHost` собирает их внутри своих destination через
  `collectAsStateWithLifecycle`.
- Добавлены целевые тесты:
  `ReportFiltersTest`, `PhotoUrlResolverTest`, `SearchCoordinatorTest`,
  `UserSessionTest`, `HomeFeedCoordinatorTest`, `FavoritesCoordinatorTest`,
  `LikesCoordinatorTest`, `MapCoordinatorTest`, `ReportDetailCoordinatorTest`.
- `./gradlew :app:testDebugUnitTest` — 232 теста, 0 падений.
- `./gradlew :app:assembleDebug` — успешно.
- `./gradlew :app:compileDebugAndroidTestKotlin` — успешно.
- `MainViewModelTest` не изменялся и проходит без правок.
