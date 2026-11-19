# Fishing — план работ по аудиту

**Дата аудита:** 2026-11-09  
**Статус:** активен  
**Текущий фокус:** `AUD-07b` — адаптивная навигация и контент (P2)
**Следующее действие:** `AUD-07b` — заменить нижнюю навигацию на адаптивную, добавить сетку ленты и ограничить ширину контента; затем `AUD-13` — переход на Navigation 3. `AUD-07a` закрыт 2026-11-19, `BUG-01` исправлен 2026-11-15, `BUG-02` исправлен 2026-11-18, `AUD-06` закрыт 2026-11-15.

> Это handoff-файл аудита. Код правится по мере выполнения задач; актуальные статусы — в таблице ниже.

## Как продолжить в новой сессии

В начале новой сессии достаточно написать:

```text
@docs/audit/ROADMAP.md Продолжаем с AUD-07b. Сначала прочитай файл и статусы.
```

Агент прочитает этот файл, увидит текущий фокус, порядок работ, решения и критерии готовности. После выполнения пункта нужно обновить его `Статус` и `Текущий фокус` здесь же.

## Принятые решения

1. **Release signing отложен до Release Gate.** Пока нет внешнего релиза, debug-подпись не блокирует разработку. Не распространять release-APK, собранный с debug-ключом. Перед первым APK для тестеров/сторов вернуться к `AUD-10`.
2. **P0 закрыты, дальше структурные P2.** ANR, изоляция кэша маркеров, приватность отчёта и действия аккаунта сделаны. `AUD-11` (backup + encrypted-сессия) закрыт 2026-11-15. `AUD-07a` помечен P1 по impact: клавиатура и системные панели перекрывают поля и кнопки — это пользовательский баг, а не косметика.
3. **R8 не делаем первым.** Сначала стабилизируем логику и приватность, затем включаем R8 с проверкой release-сборки.
4. **Backup/auth-токены закрыты в `AUD-11`.** При появлении новых приватных данных или новых каналов экспорта повторить проверку backup-правил и хранения сессии.
5. **Приватность важнее косметики.** «Сделать приватным», удаление аккаунта и очистка приватного кэша — не откладывать дальше первых двух итераций.
6. **Выбор аватара вынесен в отдельную задачу `AUD-12`.** В AUD-03 не трогаем аватар: текущий пикер и диалог имени остаются как есть до реализации отдельного экрана.
7. **`AUD-06` закрыт без черновика в Room.** Состояние формы отчёта живёт в activity-scoped `SavedStateHandle` (VM создаётся на уровне `FishingNavHost`), состояние подэкранов — в `rememberSaveable`. Этого достаточно для process death внутри живой задачи; полноценный черновик (вернуться через неделю, с фото) — отдельное решение, когда понадобится: тогда же передавать пути фото черновика в `keepPaths`, а не полагаться на 7-дневное окно. Ориентация временно зафиксирована в portrait на телефоне; `AUD-07a` снимает этот лок, restore-логика к этому готова.
8. **Порядок перестроен под «релиз ещё далеко».** Сначала задачи, которые меняют структуру приложения и иначе вызовут переделку (`AUD-11` как базовая приватность, затем `AUD-06`, `AUD-07a`, `AUD-07b`, `AUD-13`, `AUD-12`), потом изолированные улучшения (`AUD-08`), затем инфраструктура и release-подготовка (`AUD-09`, `AUD-05`, `AUD-10`). `AUD-05` разделён: R8/иконки/шрифты — до первых тестеров, baseline profile — на замороженном UI перед Release Gate.

9. **`AUD-07` разделён на `AUD-07a` и `AUD-07b`.** IME/insets — исправление уже существующего поведения (P1), адаптивная навигация и сетки — изменение структуры UI (P2). У них разный риск, разные проверки и разные критерии готовности; смешивать в одну задачу нельзя.

10. **Переход на Navigation 3 — отдельная задача `AUD-13`.** `NavigationSuiteScaffold` и адаптивные сетки не требуют Nav3, поэтому `AUD-07b` не блокируется миграцией. Nav3 нужен только для multi-pane (`SceneStrategy`); делаем его после стабилизации layout'а и до новых экранов (`AUD-12`), чтобы не переписывать маршруты дважды.

## Очередь работ

| ID | Приоритет | Статус | Задача |
|---|---|---|---|
| AUD-11 | P0 | `DONE` | Backup + plaintext Supabase-сессия + приватные данные |
| AUD-06 | P2 | `DONE` | Сохранение состояния форм при повороте/process death |
| AUD-07a | P1 | `DONE` | Edge-to-edge, insets и IME; снятие portrait-lock |
| AUD-07b | P2 | `TODO` | Адаптивная навигация, сетки и максимальная ширина контента |
| AUD-13 | P2 | `TODO` | Переход на Navigation 3 (структурная подготовка к multi-pane) |
| AUD-12 | P2 | `TODO` | Экран выбора аватара и сохранение без диалога имени |
| AUD-08 | P2 | `TODO` | Nominatim: клиент, User-Agent, таймауты |
| AUD-09 | P2 | `TODO` | Окружения, baseline-миграции Supabase, CI |
| AUD-05 | P2 | `TODO` | R8, shrink resources, размер APK, baseline profile (A — до тестеров, B — перед Release Gate) |
| AUD-10 | Release Gate | `DEFERRED` | Release signing: keystore, Play App Signing, секреты |

**Порядок и зависимости новых задач:**
- `AUD-07a` — можно начинать сразу, блокеров нет.
- `AUD-07b` — после `AUD-07a`: сначала фиксируем владельца insets (`Scaffold` vs `NavigationSuiteScaffold`), иначе получим двойные отступы.
- `AUD-13` — после `AUD-07b`, когда каркас экранов стабилен. `AUD-07a` и `AUD-07b` от него не зависят; Nav3 разблокирует только multi-pane list-detail.
- `AUD-12` — рекомендуется после `AUD-13`, чтобы новый экран сразу жил в целевой навигации.

Закрытые задачи: `AUD-01` (ANR профиля), `AUD-02` (кэш маркеров), `AUD-03` (приватность/аккаунт), `AUD-04` (очистка приватного отчёта), `AUD-11` (backup/сессия), `AUD-06` (состояние форм), `AUD-07a` (insets/IME).

**Почему такой порядок:** `AUD-07a` идёт первым, потому что IME/insets — регресс, видимый на каждом телефоне, и он не зависит от структуры навигации. `AUD-07b` меняет каркас `MainShell` и владение отступами, поэтому идёт сразу после 07a. `AUD-13` — структурная миграция навигации: её выгодно сделать до новых экранов (`AUD-12`), иначе маршруты и `SceneStrategy` придётся переписывать дважды; при этом `AUD-07b` от неё не зависит. `AUD-08` изолирован и не блокирует остальное. `AUD-09`/`AUD-05`/`AUD-10` — release-подготовка; до релиза далеко, поэтому они в конце, но `assembleRelease` должен оставаться рабочим, как только включим R8.

## Баги (сообщены 2026-11-15, 2026-11-18)

> Найден при ручной проверке на устройстве после `AUD-06`. Причина подтверждена, баг исправлен 2026-11-15 (см. ниже).

### BUG-01 — Возврат из системной камеры выбрасывает из формы отчёта на главный экран (FIXED, 2026-11-15)

**Симптом:** в форме нового отчёта открыть «Добавить фото» → «Камера» → системная камера открывается. Нажатие **любой** кнопки в камере («Отмена» или затвор/подтверждение снимка) сразу выбрасывает приложение на главный экран.

**Воспроизведение:**
1. «Новый отчёт» → «Добавить фото» → «Камера»;
2. в системной камере нажать «Отмена» **или** снять кадр и подтвердить;
3. ожидаемо: возврат в ту же форму отчёта с сохранёнными данными; фактически: главный экран.

**Где искать:**
- `app/src/main/java/com/example/fishing/ui/screens/report/create/CreateReportScreen.kt:181-206` — `TakePicture`, `beginCameraCapture`, выдача `FileProvider`-URI;
- `app/src/main/java/com/example/fishing/viewmodel/CreateReportViewModel.kt:747-848` — `onCameraCaptureResult`, `publishCameraPhoto`, токен `currentReportId`;
- `app/src/main/java/com/example/fishing/utils/PhotoUtils.kt` — `createTempPhotoFile`, `persistCameraPhoto`, пережатие JPEG;
- `app/src/main/java/com/example/fishing/MainActivity.kt:58-72` — экран загрузки, пока `authState is AuthState.Loading`;
- `app/src/main/java/com/example/fishing/ui/navigation/FishingNavHost.kt:86-88` — `rememberNavController()` внутри `key(isAuthenticated)`;
- `AndroidManifest.xml` (FileProvider) и `app/src/main/res/xml/file_paths.xml`.

**Проверить в первую очередь:** стек `AndroidRuntime`/`RuntimeException` в logcat; права и место в `cacheDir`; не пустой ли файл после камеры; не падает ли `SavedStateHandle` при уходе в камеру (`onSaveInstanceState`); не регрессия ли `AUD-06` (`publishCameraPhoto`, `recoverInterruptedCameraCapture`).

**Вероятная причина (подтвердить на устройстве).** Похоже, при возврате из камеры Activity/процесс пересоздаются, а навигационный back stack не восстанавливается:
- `MainActivity.kt:58-72` показывает только спиннер, пока `authState is AuthState.Loading`, и `FishingNavHost` в этот момент **не входит в композицию**;
- `FishingNavHost.kt:86-88` создаёт `rememberNavController()` внутри `key(isAuthenticated)`, а `authState` стартует с `AuthState.Loading` (`SupabaseAuthRepository.kt:86`, `SharingStarted.WhileSubscribed`);
- когда сессия догружается и `FishingNavHost` появляется снова, его сохранённое состояние навигации уже не к чему восстановить — стартует `MainRoute` (главный экран). Форма отчёта и её маршрут теряются, хотя `CreateReportViewModel` (activity-scoped) может сохранить состояние формы в `SavedStateHandle`.

Проверить, включена ли на устройстве опция Developer options → «Don't keep activities»; но баг актуален и при обычном kill процесса в фоне.

**План исправления:**
- поднять `NavHostController` в `MainActivity`/корень `setContent` и передавать его в `FishingNavHost`, чтобы контроллер жил вне ветки `Loading`;
- не убирать `NavHost` из композиции во время `Loading` — рисовать спиннер оверлеем поверх, тогда `rememberSaveable`/saved state навигации восстанавливается;
- убрать `key(isAuthenticated)`; вместо пересоздания NavHost реагировать на переходы auth эффектом (`LaunchedEffect`) с `navigate(...) { popUpTo(0) }` только при реальной смене пользователя (вход/выход), не трогая восстановленный стек;
- `startDestination` зафиксировать (`MainRoute`), при `Unauthenticated` — редирект на `LoginRoute`;
- проверить сценарии: возврат из камеры (отмена/снимок), process death на форме отчёта, logout→login, обычный холодный старт;
- добавить регресс: тест на восстановление nav-состояния (если возможно в Robolectric) или явный ручной чек-лист в этом баге.

**Готово, когда:** камера (отмена и снимок) возвращает в ту же форму отчёта с сохранёнными данными; back stack переживает process death; logcat чистый; `:app:testDebugUnitTest` зелёный.

**Исправлено (2026-11-15):**
- `NavHost` больше не покидает композицию при `AuthState.Loading`: `MainActivity` держит его всегда, а спиннер рисуется оверлеем. Оверлей перехватывает тапы и системный back, скрывает контент от TalkBack, поэтому под ним нельзя случайно нажать невидимую кнопку или открыть диалог отмены.
- `showDiscardDialog`/`formHasData` теперь недоступны, пока идёт восстановление сессии.
- `NavHostController` живёт всю жизнь композиции: `key(isAuthenticated)` убран, `startDestination = SplashRoute`, переход на `MainRoute`/`LoginRoute` делает `AuthNavigationEffect` по `resolveAuthNavigation`. Восстановленный стек (`CreateReportRoute`, `ReportDetailRoute` и т.п.) не сбрасывается; вход/выход чистят стек через `popUpTo(navController.graph.id)`.
- Загрузки, которым нужна сессия (`ReportDetailRoute`, `UserProfileRoute`, `ReportListSearchRoute`, `FullMapRoute`), гейтятся `SessionReadyEffect`: при `Loading` запрос не стартует, после готовности сессии выполняется один раз (иначе восстановленный экран мог навсегда остаться в `Loading`).
- Геолокация запрашивается только при первом создании Activity (`savedInstanceState == null`), а не при каждом возврате из камеры.
- Тесты: `AuthNavigationTest` (10), `AuthRouteTest` (10), `AuthNavigationEffectTest` (6, реальный `NavHostController`), `FishingAppContentTest` (7, включая «оверлей съедает тап» и «back не доходит до контента»), `SessionReadyEffectTest` (3). Мутационно проверено, что тесты падают при возврате старого поведения.
- `:app:testDebugUnitTest --rerun-tasks` — 456 тестов, 0 падений; `assembleDebug` зелёный.

**Осталось вручную (device):** камера → «Отмена»/снимок возвращает в форму с данными; process death на форме (`Don't keep activities`/`am kill`) сохраняет стек и состояние формы; TalkBack под спиннером читает только прогресс; холодный старт авторизованного без вспышки логина; logout→login очищает стек; экран деталей после process death догружает данные.

### BUG-02 — Возврат с карты в детали отчёта показывает спиннер (FIXED, 2026-11-18)

**Симптом:** детали отчёта → тап по локации → full-map → системный/экранный back: детали «перезагружаются на мгновение» — вместо готового отчёта на кадр появляется полноэкранный `CircularProgressIndicator`, затем отчёт возвращается (и, вероятно, скролл прыгает наверх).

**Воспроизведение:**
1. открыть отчёт (лента или карта) → экран деталей;
2. нажать на секцию локации → открывается карта;
3. сразу нажать «назад»;
4. ожидаемо: детали на месте без индикатора загрузки; фактически: вспышка спиннера.

То же самое при возврате из профиля автора в детали (детали → автор → назад).

**Где искать:**
- `app/src/main/java/com/example/fishing/ui/navigation/FishingNavHost.kt:398-412` — destination `ReportDetailRoute`, `SessionReadyEffect(reportId, authState) { viewModel.loadReportDetails(it) }`, `FishingNavHost.kt:444-447` — переход на `FullMapRoute`;
- `app/src/main/java/com/example/fishing/viewmodel/detail/ReportDetailCoordinator.kt:61-77` — `loadReportDetails`;
- `app/src/main/java/com/example/fishing/ui/screens/report/detail/ReportDetailScreen.kt:359-390` — `ReportDetailLoadingScreen`;
- для сравнения: `app/src/main/java/com/example/fishing/viewmodel/user/UserProfileCoordinator.kt:52-65` — такой же guard уже был сделан для профиля автора.

**Причина (подтверждена).** `MainViewModel` activity-scoped, поэтому данные деталей при уходе на карту не теряются: в `reportDetailUiState` остаётся `Success`. Но композиция destination'а уничтожается, а при возврате `LaunchedEffect` внутри `SessionReadyEffect` стартует заново (ключи те же, но это новая композиция). `loadReportDetails` безусловно выставлял `_reportDetailUiState.value = ReportDetailUiState.Loading`, хотя на экране уже был этот же отчёт. Ветка `when` менялась `Success → Loading → Success`: на время запроса Room-кэша рисовался полноэкранный спиннер. Поскольку `popEnter`-анимация длится 300 мс, спиннер въезжал вместе с ней, а не мигал один кадр.

Побочно: переключение ветки `Success → Loading → Success` уничтожает поддерево с `verticalScroll(rememberScrollState())`, поэтому позиция скролла после вспышки не восстанавливается; кроме того, каждый возврат заново вызывал `repository.refreshReportDetails(id)` — лишний сетевой запрос.

**Исправление (2026-11-18):**
- `ReportDetailCoordinator.loadReportDetails` оставляет `Success`, если на экране уже этот же отчёт, и только для другого отчёта (или после `clear()`) выставляет `Loading`. Фоновый авторитетный refresh и обработка `Missing`/`NoSession` не изменились;
- поведение выровнено с `UserProfileCoordinator` и `MapCoordinator.loadMapMarkers`, где такой же guard уже был;
- тесты в `ReportDetailCoordinatorTest`: «returning to the same report keeps it on screen without loading» (падает на старом коде, проверено мутационно) и «opening another report still shows the loading state»;
- `:app:testDebugUnitTest` — 458 тестов, 0 падений.

**Осталось вручную (device):** детали → карта → назад без спиннера и с сохранённой позицией скролла; детали → профиль автора → назад так же; приватный/удалённый отчёт по-прежнему даёт `Unavailable`; process death на деталях по-прежнему грузит отчёт со спиннером.

> Терминология: в UI «черновик» — это **неопубликованный отчёт** (бейдж «Не опубликован», снекбар «Сохранено в черновики», `R.string.report_draft_saved_notice`). Это не сохранённая незавершённая форма. Списка черновиков в приложении нет; состояние формы живёт только в `SavedStateHandle` до отправки или сброса.

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

**Остаточный риск (закрыт в AUD-04):** если отчёт был публичным при кэшировании, а потом стал приватным на сервере, маркер жил до сетевого refresh. Теперь авторитетное «отчёта нет» удаляет строку `report_details` и маркер.

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
- Смена пароля: отдельный маршрут `ChangePasswordRoute` и экран `ChangePasswordScreen`; `ChangePasswordViewModel` валидирует пароль и совпадение, вызывает `auth.updateUser { password = ... }`, показывает загрузку/ошибку и snackbar об успехе. Диалог с текстовыми полями не использовали: он вызывал зависание Compose в Robolectric.
- Удаление аккаунта: подтверждающий диалог в настройках профиля, `AuthRepository.deleteAccount()` вызывает RPC `public.delete_account()`, затем очищает локальные кэши отчётов/избранного/лайков/маркеров и очередь загрузки. Новая миграция `supabase/migrations/20261110090000_account_deletion.sql` удаляет отчёты, профиль и строку `auth.users` только для `auth.uid()`.
- Тесты: `ReportDetailScreenUiTest` (пункт редактирования отсутствует; приватность сначала показывает dialog с подтверждением; отмена ничего не отправляет; подтверждение шлёт противоположное состояние; успех показывается в snackbar; действие заблокировано во время запроса), `ReportDetailCoordinatorTest` (успех/ошибка смены видимости и результат), `MainViewModelTest` (проксирование, результат и обновление кэша), `EditProfileViewModelTest` (удаление аккаунта), `ChangePasswordViewModelTest`, `EditProfileScreenUiTest`, `ChangePasswordScreenUiTest`.
- `:app:testDebugUnitTest` — 298 тестов, 0 падений.

**Обновлено (2026-11-15): политика паролей приведена к серверной.** До этого клиент проверял `< 6` и при отказе сервера всегда показывал «минимум 6 символов», хотя проект требует 10 символов + lowercase/uppercase/digits (`docs/security/rls-audit-FJ-4.md`, F-6). Что сделано:
- `data/PasswordPolicy.kt` — единый источник правды: `MIN_LENGTH = 10`, проверки строчной/заглавной/цифры, `validate()` с конкретным сообщением. Используется в `ChangePasswordViewModel` и `RegistrationViewModel`.
- `ChangePasswordScreen` переработан по макету Figma (4144:6178): «Сохранить» — пилюля в `TopAppBar` (disabled/активный/спиннер), eye-переключатели во всех полях, вместо статичной подсказки — живой чек-лист из 4 пунктов, зелёный цвет «выполнено» вынесен в `FishingTheme.colors.success` (light/dark). Кнопка активна только когда все правила выполнены и пароли совпадают.
- **Текущий пароль обязателен.** Проект включает `UpdatePasswordRequireCurrentPassword`: голый `updateUser { password = ... }` возвращает `current_password_required`. Экран получил третье поле «Текущий пароль», `AuthRepository.changePassword(newPassword, currentPassword)` передаёт его в `updateUser { currentPassword = ... }`. Локально проверяются пустое значение и совпадение нового пароля с текущим.
- **Обход бага supabase-kt (3.6–3.9).** У `UserUpdateBuilder.currentPassword` нет `@SerialName`, поэтому `updateUser` отправляет `currentPassword`, сервер его игнорирует и отвечает `current_password_required` даже при заполненном поле (проверено тестом сериализации). `SupabaseAuthRepository.updatePassword` больше не использует `updateUser`: она повторяет его через `authenticatedSupabaseApi(supabase.auth).putJson("user", ...)` с корректным ключом `current_password` (тело собирает `passwordUpdateBody`, покрыто тестом). Вернуть SDK-вызов после исправления в библиотеке.
- `ErrorMapper` больше не хардкодит 6: вытаскивает `N` из `Password should be at least N characters`, переводит `should contain at least one character of each`, `current_password_required` / `current_password_mismatch` (различаются по коду, текст у них одинаковый), `same_password`, `reauthentication_needed`, «слабый пароль». Нераспознанная ошибка `RestException` больше не выводится целиком: вместо URL/заголовков показывается короткое «Не удалось выполнить действие. Попробуйте позже».
- Тесты: `PasswordPolicyTest`, `ErrorMapperTest`, обновлены `ChangePasswordViewModelTest` (порог 10, текущий пароль, отдельные проверки букв/цифры), `ChangePasswordScreenUiTest` (три поля, live-чек-лист), `RegistrationViewModelTest` (валидный пароль `Password123`, отдельный тест на цифру).
- `:app:testDebugUnitTest` — 347 тестов, 0 падений.

**Обновлено (2026-11-15):** миграция `20261110090000_account_deletion.sql` применена к Supabase-проекту, RPC `public.delete_account()` доступна. Блокер «функция не найдена» снят. Удаление аккаунта проверено вручную на устройстве и в Supabase: аккаунт и его данные удаляются, ошибок нет.

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

**Сделано (2026-11-15):**
- `ReportDetailsStore.refreshReportDetails` больше не возвращает `Boolean`, в котором смешивались «нет сессии» и «отчёта нет». Новый `ReportRefreshResult` (`Found` / `Missing` / `NoSession`) отделяет авторитетный отрицательный ответ от отсутствия сессии; сетевые сбои по-прежнему бросают исключение.
- При `Missing` `RoomReportDetailsStore` в одной транзакции удаляет строку `report_details` **и** маркер из `markers`: приватный отчёт и его координаты больше не остаются в офлайн-кэше и на карте. `NoSession` кэш не трогает.
- `refreshReportDetails` перенесён в интерфейс `FishingRepository` (раньше был отдельным методом `SupabaseFishingRepository`, из-за чего координатор проверял тип через `is`). Это убрало type-check и позволило тестировать сценарий на фейке.
- `ReportDetailCoordinator`: кэш из Room по-прежнему рисуется сразу, но `Empty` теперь выставляется и поверх `Success`, если сервер ответил `Missing`. Флаг `detailUnavailable` и инкремент `detailPhotosVersion` не дают запоздавшей эмиссии Room или резолву фото «воскресить» отчёт. Ошибка сети и `NoSession` оставляют закэшированный отчёт на экране.
- Тесты: `SupabaseFishingRepositoryReadTest` (кэш + маркер удаляются при `Missing`; `NoSession` сохраняет кэш; `Found` кэширует), `ReportDetailCoordinatorTest` (`Success → null` даёт `Empty`; сетевая ошибка и отсутствие сессии сохраняют `Success`).
- `:app:testDebugUnitTest` — 352 теста, 0 падений.

### AUD-05 — R8 и размер APK

**Проблема:** `isMinifyEnabled = false`, `isShrinkResources` не задан, `proguard-rules.pro` пустой, APK ~63 МБ, 4 dex, ~16 МБ шрифтов.

**Где:**
- `app/build.gradle.kts:32-40`
- `app/build.gradle.kts:61`
- `app/src/main/java/com/example/fishing/ui/theme/Type.kt:12-21`
- `app/build/outputs/apk/release/app-release.apk`

**Что сделать:** включить оптимизацию R8, ресурсный shrink, заменить `material-icons-extended`, сабсетить шрифты, добавить baseline profile; проверить release-сборку на устройстве.

**Части:**
- **A — до первых тестеров:** R8 + `shrinkResources`, `material-icons-extended` → `icons-core`/точечные иконки, сабсет шрифтов, keep-правила. Не срочно для ежедневной разработки (debug-сборка не минифицируется), но release-APK должен собираться и запускаться.
- **B — перед Release Gate:** baseline profile (macrobenchmark на устройстве/эмуляторе) и контрольные замеры startup/scroll на замороженном UI.

**Готово, когда:**
- release-сборка проходит и запускается;
- Hilt/Room/Serialization/Supabase не падают в release;
- размер APK существенно меньше;
- есть замер startup/scroll до и после (часть B).

### AUD-06 — Сохранение состояния форм

**Проблема:** `WaterEdit`, `CommentEdit`, `CatchEdit`, `MethodAndBait`, `EditProfile` используют `remember`; `CreateReportViewModel` не использует `SavedStateHandle`; путь к временному фото камеры тоже в `remember`.

**Где:**
- `app/src/main/java/com/example/fishing/ui/screens/report/create/WaterEditScreen.kt:60-62`
- `app/src/main/java/com/example/fishing/ui/screens/report/create/CommentEditScreen.kt:41`
- `app/src/main/java/com/example/fishing/ui/screens/report/create/CatchEditScreen.kt:32-38`
- `app/src/main/java/com/example/fishing/ui/screens/report/create/FishingMethodAndBaitScreen.kt:18-19`
- `app/src/main/java/com/example/fishing/ui/screens/profile/EditProfileScreen.kt:47-48`
- `app/src/main/java/com/example/fishing/ui/screens/report/create/CreateReportScreen.kt:141`

**Что сделать:** `rememberSaveable`/`SavedStateHandle` для состояния формы и temp-файла; при необходимости — отдельное будущее решение: сохранение черновика в Room/DataStore.

**Готово, когда:**
- поворот не теряет введённые данные;
- process death не теряет форму или восстанавливает состояние формы;
- результат камеры не пропадает после recreation.

**Сделано (2026-11-15):**

**Ориентация.** `MainActivity` зафиксирована в portrait (`android:screenOrientation="portrait"`), чтобы на телефоне поворот не пересоздавал Activity. Ограничение не заменяет restore-логику: process death, смена темы/шрифта/языка, мультиокно и большие экраны (Android 16 игнорирует лок при smallest width ≥ 600dp) по-прежнему пересоздают UI — учесть в `AUD-07a`/`AUD-07b`.

**Форма отчёта.** `CreateReportViewModel` получает `SavedStateHandle` последним параметром (со значением по умолчанию, чтобы не ломать тесты/preview). Все поля формы переведены на делегаты с ключами `form_*` (`SavedStateProperty`, `ReportFormCodecs`): `GeoPoint` — как `form_geo_lat`/`form_geo_lng`, фото — `id + uri`, `Fish`/`Bait`/`FishingMethod` — enum-именами. `currentReportId` тоже сохраняется, поэтому повторный Save после process death делает upsert по тому же id без дубля. Дефолтные даты выставляются только при отсутствии `form_start_date`; `saveState`/`saveProgress`/`pendingProcessedPaths` транзиентные (после восстановления `Idle`). Сброс формы — только на переходе `Authenticated → Unauthenticated`, а не на холодном старте с `Unauthenticated`. Состояние формы привязано к пользователю (`form_owner_id`).

**Камера и фото.** Владение temp-файлом камеры переехало в VM (`beginCameraCapture`/`onCameraCaptureResult`/`recoverInterruptedCameraCapture`): после process death непустой temp-файл публикуется при старте, пустой/отсутствующий удаляется, поздний колбэк не дублирует фото. Восстановленные фото проверяются на читаемость, недоступные отбрасываются с snackbar, для `content://` освобождается грант. `isRestoringPhotos`/`isPublishingCameraPhoto` блокируют Save, пока фото не готовы. `takePersistableUriPermission` при выборе из галереи (best-effort) и release при удалении/сбросе/сохранении; при logout `clearSessionPhotos` освобождает все persisted-гранты. `PhotoUtils.clearSavedPhotos` получил `graceMs`: при старте — 7 дней (незавершённая форма может открыться позже), при logout — 0. `removePhoto` удаляет файл только внутри `filesDir/photos` и не трогает пути из FAILED-очереди.

**Подэкраны и профиль.** `rememberSaveable`: `WaterEdit`, `CommentEdit`, `CatchEdit` (включая `SnapshotStateList<Fish>` через `Saver`), `FishingMethodAndBait`, `CreateReportScreen.isDetailsExpanded`, `EditProfile` (имя, флаг ручного редактирования, диалог имени). Флаги диалогов намеренно остаются `remember`. `LaunchedEffect(initialName)` больше не затирает ручное/восстановленное имя (`resolveNameSync`); `selectedPhotoUri` оставлен в `remember`, потому что грант пикера не переживает process death.

**Проверка.** `:app:testDebugUnitTest --rerun-tasks` — 420 тестов, 0 падений (несколько полных прогонов), `assembleDebug` зелёный. Покрыты восстановление всех полей и дат, `Idle` после restore, переиспользование `currentReportId`, recovery/битый/0-байтный temp-файл, гонка «камера → сброс», потерянные фото, смена пользователя, strict/grace cleanup, восстановление подэкранов (`StateRestorationTester`), snackbar (тест падает при откате порядка show/consume).

**Известные ограничения и follow-up:**
- пути фото незавершённой формы не передаются в `keepPaths`: незавершённая форма, открытая позже 7 дней, потеряет файлы (Room-черновик сознательно не делали);
- `EditProfile`: восстановление имени/аватара и confirm диалога UI-тестом под Robolectric не проверяются (OOM `ShadowLineBreaker`) — нужен device/instrumentation тест;
- повторная доставка результата фото-пикера после process death может дать ложный snackbar — `TODO(AUD-06, manual check)` в `CreateReportScreen`;
- ниты review: `isPublishingCameraPhoto` — Boolean, не считает параллельные публикации; `referencedLocalPaths()` в `removePhoto` fail-open (при ошибке БД файл удалится).

**Ручная проверка на устройстве (осталось):** Developer options → Don't keep activities; `adb shell am kill com.example.fishing` с заполненной формой, с открытой камерой и с выбранными из галереи фото; logout после выбора фото; возврат из «недавних» через сутки+.

### AUD-07a — Edge-to-edge, insets и IME (P1)

**Проблема:** `enableEdgeToEdge()` и `adjustResize` уже включены, но экраны не доводят эту модель до конца: клавиатура перекрывает поля ввода и кнопки, списки поиска уходят под navigation bar, FAB на full-map не учитывает системную панель. Дополнительно `MainActivity` залочена в portrait, что противоречит `adaptive`-скиллу и мешает планшетам/складным.

**Почему P1:** это не косметика. На маленьком экране с открытой клавиатурой пользователь не видит поле или кнопку «Сохранить» — сценарий ввода пароля/отчёта ломается.

**Где:**
- `app/src/main/AndroidManifest.xml:26` — `android:screenOrientation="portrait"`;
- `app/src/main/java/com/example/fishing/ui/screens/login/LoginScreen.kt:84-90`;
- `app/src/main/java/com/example/fishing/ui/screens/login/RegistrationScreen.kt:121-122`;
- `app/src/main/java/com/example/fishing/ui/screens/profile/EditProfileScreen.kt:187`;
- `app/src/main/java/com/example/fishing/ui/screens/profile/ChangePasswordScreen.kt:187-188` — `imePadding` после `verticalScroll`;
- `app/src/main/java/com/example/fishing/ui/screens/report/create/WaterEditScreen.kt:153`;
- `app/src/main/java/com/example/fishing/ui/screens/report/create/CatchEditScreen.kt:149`;
- `app/src/main/java/com/example/fishing/ui/screens/report/create/CommentEditScreen.kt:83`;
- `app/src/main/java/com/example/fishing/ui/screens/search/ReportSearchScreen.kt:181-204`;
- `app/src/main/java/com/example/fishing/ui/screens/search/LocationSearchScreen.kt:75-104`;
- `app/src/main/java/com/example/fishing/ui/screens/map/MapScreen.kt:437-449` — FAB в `FullMapRoute`.

**Что сделать:**
- убрать `android:screenOrientation="portrait"`; поворот и fold должны пересоздавать UI без потери формы (регресс `AUD-06`);
- для login/registration и всех экранов с `TextField` добавить `imePadding()` (или `fitInside(WindowInsetsRulers.Ime.current)`) **до** `verticalScroll`; не дублировать IME, если `contentWindowInsets` уже содержит его;
- в поисковых экранах оставить `contentWindowInsets = WindowInsets(0, 0, 0, 0)`, но добавить нижний inset navigation bar в `contentPadding` списка; `imePadding` не убирать, чтобы не получить двойной отступ;
- в `MapScreen` для standalone/full-map добавить `navigationBarsPadding()` к FAB; в табе учесть, что после `AUD-07b` контент станет edge-to-edge и отступ больше не будет приходить снаружи;
- пройти по всем `Scaffold` (21 шт.) и `TextField` (10 файлов), а не только по списку из аудита: сверить `innerPadding`, `consumeWindowInsets` и отсутствие двойных отступов;
- оставить `enableEdgeToEdge()` и `windowSoftInputMode="adjustResize"` как есть.

**Готово, когда:**
- клавиатура не перекрывает поля и кнопки на login, registration и всех формах;
- списки поиска и FAB full-map не пересекаются с navigation bar;
- поворот телефона и fold не теряют введённые данные (существующие тесты `AUD-06` + ручная проверка);
- нет двойных отступов на экранах с `Scaffold`/`imePadding`;
- `:app:testDebugUnitTest` и `assembleDebug` зелёные.

**Оценка:** 1.5–2 дня. **Не входит:** rail/адаптивные сетки (`AUD-07b`), Nav3 (`AUD-13`), screenshot-тесты (backlog).

**Ход выполнения (2026-11-19, ветка `audit/aud-07a-edge-to-edge`):**
- Снят `screenOrientation="portrait"`; `enableEdgeToEdge()` и `adjustResize` сохранены.
- Для прозрачной навигационной панели отключён принудительный системный контраст после `enableEdgeToEdge()`.
- Login, registration и формы редактирования получили обработку IME до `verticalScroll`; padding от `Scaffold` потребляется явно. В форме отчёта сохранён существующий `imePadding`, добавлено потребление `innerPadding`.
- Запрос поиска места переведён на `rememberSaveable`, чтобы не терять введённый текст при смене ориентации.
- Поиск отчётов и мест оставляет ручной top inset, добавляет нижний inset navigation bar спискам только когда IME скрыта; список по-прежнему поднимается над клавиатурой.
- FAB standalone/full-map учитывает navigation bar; в табе отступ остаётся владельцем `MainShell`/`Scaffold`, чтобы не удваивать его.
- Проверено статически: все 21 `Scaffold` и поля ввода в Compose; остальных мест с `imePadding`/`contentWindowInsets` двойного применения не обнаружено.
- `ANDROID_HOME=/Users/vital/Library/Android/sdk ./gradlew :app:testDebugUnitTest :app:assembleDebug` — `BUILD SUCCESSFUL`.
- **Ручная проверка на устройстве (2026-11-19):** владелец подтвердил, что сценарии работают. Первичная проверка ориентации запускала старую сборку из Android Studio; после установки APK из ветки поворот работает.

### AUD-07b — Адаптивная навигация и контент (P2)

**Проблема:** `MainShell` всегда рисует `NavigationBar` снизу, лента — одна колонка, у форм нет максимальной ширины. На планшете и развёрнутом foldable это выглядит как растянутый телефон.

**Где:**
- `app/src/main/java/com/example/fishing/ui/screens/main/MainShell.kt:129-230` — `Scaffold` + `NavigationBar`;
- `app/src/main/java/com/example/fishing/ui/screens/main/HomeTabContent.kt:133-141` — `LazyColumn`;
- `app/src/main/java/com/example/fishing/ui/screens/login/LoginScreen.kt:84`;
- формы профиля/отчёта (`EditProfile`, `CreateReport`, `WaterEdit`, `CatchEdit`, `CommentEdit`);
- детали отчёта и поисковые списки — для максимальной ширины.

**Что сделать:**
- добавить `androidx.compose.material3:material3-adaptive-navigation-suite` (версия из Compose BOM);
- заменить `Scaffold.bottomBar` на `NavigationSuiteScaffold`: на телефоне — `NavigationBar`, на ≥ 600dp — `NavigationRail`; пункты те же (`BottomNavItem`);
- сохранить `TopAppBar` и текущее поведение pager'а; помнить, что `NavigationSuiteScaffold` **не передаёт `PaddingValues`** — каждый экран сам владеет нижними/боковыми insets;
- пересчитать позиционирование snackbar и `ReportUploadBanner` в `MainShell.kt:306-362` под rail/bar;
- `HomeTabContent`: `LazyColumn` → `LazyVerticalGrid(GridCells.Adaptive(320.dp))`, сохранить `PullToRefreshBox`, пагинацию и `contentType`; для служебных элементов использовать `span = maxLineSpan`;
- ограничить ширину контента: login/registration ~480dp, формы профиля/отчёта ~640dp, детали ~840dp, выравнивать по центру;
- проверить phone portrait/landscape, tablet, unfolded foldable; на `w411dp` — bar, на `w840dp` — rail и 2+ колонки.

**Готово, когда:**
- на телефоне нижняя навигация, на планшете/развёрнутом foldable — rail;
- лента на широком экране показывает 2+ колонки, пагинация и pull-to-refresh работают;
- формы и детали не растянуты на всю ширину;
- snackbar/баннер загрузки не перекрываются навигацией;
- добавлены Robolectric-тесты на `w411dp`/`w840dp` (bar vs rail, число колонок);
- `:app:testDebugUnitTest` и `assembleDebug` зелёные.

**Оценка:** 2–3 дня. **Зависит от:** `AUD-07a`. **Не входит:** multi-pane list-detail (нужен `AUD-13`), desktop/tabletop-сценарии.

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

**Сделано (2026-11-15):**
- **Backup выключен.** `android:allowBackup="false"`. Дополнительно `backup_rules.xml` и `data_extraction_rules.xml` явно исключают все домены (`root`/`file`/`database`/`sharedpref`/`external` и `device_*`) для cloud-backup и device-transfer. Это нужно потому, что на Android 12+ `allowBackup=false` у части производителей отключает только облако, но не D2D-перенос; правила закрывают и его, и любое будущее включение backup. По той же причине сессия хранится в `noBackupFilesDir`, который система не бэкапит никогда.
- **Encrypted session manager.** Новый пакет `data/auth/`: `KeystoreSessionCipher` (AES-256-GCM, ключ в Android Keystore, формат `[version][ivSize][iv][ciphertext]`) и `EncryptedSessionManager` (`SessionManager` из supabase-kt). Файл `auth/session.bin` пишется атомарно через `.tmp` + rename, доступ сериализован `Mutex`, I/O идёт на `Dispatchers.IO`. Повреждённый файл или инвалидированный ключ (например, после восстановления на другом устройстве) удаляется, и приложение просит войти заново, а не падает на старте.
- **Миграция со старого plaintext-хранения.** На первом `loadSession` читается ключ supabase-kt из `SharedPreferences` (`sb-<url>-session`), сессия пересохраняется в зашифрованный файл, plaintext-запись удаляется через `commit()`. Если запись не удалась, plaintext остаётся до следующего запуска — пользователя не разлогинивает молча. `deleteSession` (logout, отзыв токена, удаление аккаунта) чистит и файл, и legacy-ключ.
- **E-mail убран из pending payload.** `PendingUserPayload` больше не содержит `email`: очередь загрузки лежит в plaintext Room, а для публикации нужны только id, имя и аватар. Старые строки очереди декодируются благодаря `ignoreUnknownKeys`, `email` восстанавливается как `""`; версия схемы не менялась.
- Тесты: новый `EncryptedSessionManagerTest` (round trip и отсутствие токенов в файле, атомарная перезапись, удаление, повреждённый файл, миграция legacy и очистка plaintext, сохранение plaintext при сбое миграции, ключ совпадает с supabase-kt), обновлён `PendingReportPayloadCodecTest` (e-mail не сериализуется, старый payload с e-mail читается).
- `:app:testDebugUnitTest` — 364 теста, 0 падений.

**Остаточное:** сам Keystore-путь (`KeystoreSessionCipher`) нельзя проверить JVM-тестами — нужен запуск на устройстве/эмуляторе: вход, перезапуск приложения с сохранённой сессией, logout, обновление поверх старой сборки с plaintext-сессией. Проверить при следующей ручной проверке на устройстве.

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

### AUD-13 — Переход на Navigation 3

**Проблема:** проект использует Navigation Compose 2.10.2. `adaptive`-скилл требует Navigation 3 для multi-pane (`ListDetailSceneStrategy`/`SupportingPaneSceneStrategy`), поэтому list-detail и supporting pane сейчас недоступны. Чем позже делать миграцию, тем больше экранов и тестов придётся переписывать.

**Где:**
- `app/src/main/java/com/example/fishing/ui/navigation/FishingNavHost.kt` — `NavHost`, `composable<T>`, переходы и `savedStateHandle`;
- `app/src/main/java/com/example/fishing/ui/navigation/Routes.kt` — типизированные маршруты;
- `app/src/main/java/com/example/fishing/ui/navigation/AuthNavigation.kt` — `AuthNavigationEffect`, `SessionReadyEffect`;
- `app/src/main/java/com/example/fishing/MainActivity.kt` — владение `NavHostController`;
- тесты `AuthNavigation*`, `SessionReadyEffectTest`, `FishingAppContentTest`.

**Что сделать:**
- заменить `NavHost`/`composable<T>` на `NavDisplay` + `NavKey` (типизированные ключи сохранить);
- перенести текущий back stack и переходы: auth redirect, `popUpTo`, `savedStateHandle`-обмен между формой отчёта и подэкранами;
- сохранить поведение `BUG-01` (хост не покидает композицию при `Loading`, восстановленный стек не сбрасывается) и `BUG-02` (возврат в детали без спиннера);
- добавить `sceneStrategies` только там, где реально нужен multi-pane; начинать с list-detail для ленты/деталей отчёта;
- проверить predictive back, process death, logout→login, возврат из камеры;
- обновить unit/Robolectric-тесты навигации.

**Готово, когда:**
- все текущие маршруты и сценарии входа/выхода работают как раньше;
- process death и возврат из камеры не теряют стек;
- при необходимости list-detail на планшете открывается через `SceneStrategy`, на телефоне — обычный переход;
- `:app:testDebugUnitTest` и `assembleDebug` зелёные.

**Оценка:** 3–5 дней. **Зависит от:** `AUD-07b`. **Разблокирует:** multi-pane list-detail/supporting pane (не обязательны для релиза).

## Backlog / низкий приоритет

- **Compose Preview Screenshot Testing для form factors** (phone/foldable/tablet/desktop) — Step 1 из `adaptive`-скилла; инфраструктуры в проекте нет, добавить после `AUD-07b`.
- **Multi-pane list-detail** для ленты/деталей отчёта — только после `AUD-13`; на телефоне оставить обычный переход.
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
- `lintDebug` запущен 2026-11-15: 7 ошибок (все `MissingTranslation` в `strings.xml`, уже в backlog) и 75 предупреждений; новых замечаний по AUD-11 нет.
- RLS повторно не прогонялся; использованы существующий аудит и миграции.
- 458 unit/Compose-тестов проходят (на 2026-11-18); 21 instrumented-тест проходил на устройстве 16 октября (после v13 миграции instrumented-тесты обновлены, но на устройстве не перезапускались).
- Encrypted session manager покрыт JVM-тестами на уровне логики; сам Android Keystore проверяется только на устройстве (вход → перезапуск → logout → обновление поверх сборки с plaintext-сессией).
- Ручная проверка сохранения аватара на устройстве выполнена владельцем: фото сохраняется. Сам сценарий выбора аватара остаётся прежним до `AUD-12`.
- Смена приватности, смена пароля и удаление аккаунта проверены unit/Compose-тестами. Удаление аккаунта дополнительно проверено вручную на устройстве и в Supabase 2026-11-15. Объекты Storage при удалении аккаунта не чистятся (остаточный риск, см. AUD-09).
- Выбор аватара не входит в AUD-03 и вынесен в `AUD-12`.
