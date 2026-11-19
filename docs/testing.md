# Тестирование

## Стек

| Уровень | Инструменты | Где лежат тесты |
| --- | --- | --- |
| Unit-тесты | JUnit4, `kotlinx-coroutines-test`, MockK, Robolectric | `app/src/test/java` |
| Compose UI-тесты | `androidx.compose.ui:ui-test-junit4` + Robolectric | `app/src/test/java/com/example/fishing/ui` |
| Instrumented-тесты | Compose UI Test / Espresso на устройстве | `app/src/androidTest/java` |

Compose UI-тесты запускаются локально на JVM: `createComposeRule()` хостит
`ComponentActivity` из debug-манифеста `ui-test-manifest`, а Robolectric
поднимает Android-окружение без эмулятора. Поэтому для проверки экранов не
нужны ни устройство, ни реальный Supabase.

Ключевые настройки в `app/build.gradle.kts`:

- `testOptions { unitTests { isIncludeAndroidResources = true } }` — Robolectric
  видит ресурсы и строки приложения.
- `testImplementation(libs.androidx.compose.ui.test.junit4)` — Compose-правило
  для `test`-сорсета.
- `debugImplementation(libs.androidx.compose.ui.test.manifest)` — манифест с
  `ComponentActivity` для хост-активити.
- `testImplementation(libs.androidx.espresso.core)` — фиксирует версию,
  которую транзитивно запрашивает `ui-test-junit4`.

## Команды запуска

Все unit- и Compose UI-тесты (основная команда, подходит для CI):

```bash
./gradlew :app:testDebugUnitTest
```

Только новые UI-тесты:

```bash
./gradlew :app:testDebugUnitTest \
  --tests "com.example.fishing.ui.screens.login.LoginScreenUiTest" \
  --tests "com.example.fishing.ui.screens.report.create.CreateReportScreenUiTest" \
  --tests "com.example.fishing.ui.screens.report.detail.ReportDetailScreenUiTest"
```

Один класс или один тест:

```bash
./gradlew :app:testDebugUnitTest --tests "com.example.fishing.ui.screens.login.LoginScreenUiTest"
./gradlew :app:testDebugUnitTest --tests "*.LoginScreenUiTest.successful login submits credentials"
```

HTML-отчёт: `app/build/reports/tests/testDebugUnitTest/index.html`.
JUnit XML: `app/build/test-results/testDebugUnitTest/`.

### CI

GitHub Actions workflow `.github/workflows/android-tests.yml` запускает unit-
и Compose UI-тесты:

- на каждом pull request;
- при push в `main`;
- вручную через `workflow_dispatch`.

Workflow использует JDK 17 и Android SDK, затем запускает
`./gradlew :app:testDebugUnitTest --stacktrace`. Эмулятор и Supabase credentials
не нужны; Compose UI-тесты выполняются под Robolectric. При ошибке CI сохраняет
JUnit XML и HTML-отчёты как GitHub Actions artifact `android-test-reports`.

## Что покрыто

### `LoginScreenUiTest`

`app/src/test/java/com/example/fishing/ui/screens/login/LoginScreenUiTest.kt`

- отображение формы: email, пароль, «Войти», «Создать аккаунт»;
- успешный вход с передачей введённых данных в репозиторий;
- ошибка авторизации (`Неверный email или пароль`);
- валидация пустой формы без вызова репозитория;
- состояние загрузки: кнопки заблокированы, вместо «Войти» показывается
  прогресс;
- переход на регистрацию;
- отсутствие back-кнопки на корневом экране.

### `CreateReportScreenUiTest`

`app/src/test/java/com/example/fishing/ui/screens/report/create/CreateReportScreenUiTest.kt`

- отображение основных шагов формы и изначально недоступная кнопка
  «Сохранить»;
- включение сохранения после заполнения обязательных полей;
- ошибки валидации дат начала/окончания;
- переходы из строк формы в «Водоем», «Способ ловли и наживка», «Улов» и
  «Комментарий»;
- состояние отправки: диалог «Идёт сохранение», прогресс загрузки фото,
  завершение после опустошения очереди;
- ошибка отправки через snackbar с сохранением введённых данных;
- подтверждение выхода при несохранённых данных.

### `ReportDetailScreenUiTest`

`app/src/test/java/com/example/fishing/ui/screens/report/detail/ReportDetailScreenUiTest.kt`

- основные данные отчёта: заголовок, автор, водоем, комментарий, вес, улов,
  дата/время/длительность;
- состояние черновика (badge и баннер) и отсутствие баннера у публикации;
- действия: назад, избранное, меню, удаление с подтверждением, лайк;
- отсутствие пункта «Редактировать» (функционала в приложении нет);
- смена приватности сначала показывает подтверждающий dialog с описанием
  последствий, отмена ничего не отправляет, подтверждение отправляет
  противоположное текущее состояние; успех показывается в snackbar;
- ограничение меню для чужого отчёта (нет удаления и смены приватности);
- состояние удаления;
- экраны загрузки, недоступного отчёта и ошибки с повтором.

### `EditProfileScreenUiTest`

`app/src/test/java/com/example/fishing/ui/screens/profile/EditProfileScreenUiTest.kt`

- «Изменить пароль» открывает отдельный экран;
- подтверждение удаления аккаунта и блокировка кнопки во время запроса.

### `ChangePasswordScreenUiTest`

`app/src/test/java/com/example/fishing/ui/screens/profile/ChangePasswordScreenUiTest.kt`

- отображение трёх полей (текущий, новый, повтор) и недоступная кнопка «Сохранить» до заполнения;
- живой чек-лист требований к паролю (10 символов, строчная, заглавная, цифра);
- отправка введённых паролей;
- показ ошибки валидации;
- переключение видимости пароля;
- блокировка back во время запроса.

### `PasswordUpdateBodyTest`

`app/src/test/java/com/example/fishing/data/PasswordUpdateBodyTest.kt`

- тело смены пароля отправляет `current_password` (обход бага supabase-kt с `currentPassword`);
- нового ключа `currentPassword` в теле нет.

## Fakes и изоляция от Supabase

Compose UI-тесты не создают `SupabaseClient` и не обращаются к сети:

- `FakeAuthRepository` — вход, регистрация, состояние сессии;
- `FakeFishingRepository` — отчёты, лайки, избранное;
- `FakeReportUploadQueue` — очередь загрузки: можно задать `UPLOADING` или
  `FAILED` строку и завершить отправку, очистив `pending`.

ViewModels создаются напрямую с этими зависимостями, Hilt в UI-тестах не
поднимается (`@Config(application = Application::class)`).

## Как добавить новый Compose UI-тест

1. Положите тест в `app/src/test/java/...`, не в `androidTest`, если не нужны
   реальные системные сервисы.
2. Используйте аннотации:

   ```kotlin
   import androidx.compose.ui.test.junit4.v2.createComposeRule

   @RunWith(RobolectricTestRunner::class)
   @Config(
       sdk = [36],
       application = Application::class,
       qualifiers = "w411dp-h891dp"
   )
   class MyScreenUiTest {
       @get:Rule
       val composeRule = createComposeRule()
   }
   ```

   Используйте `v2.createComposeRule()`: он работает на `StandardTestDispatcher` и
   не даёт deprecation-предупреждения. Синхронизируйтесь через `waitForIdle()`,
   `waitUntil {}` и `mainClock`, как в существующих тестах.

3. Для экранов с `AndroidView` (карты osmdroid) оберните контент в
   `CompositionLocalProvider(LocalInspectionMode provides true)`, чтобы тест
   проверял Compose-слой, а не тайлы карты.
4. Ищите элементы сначала по семантике (`onNodeWithText`,
   `onNodeWithContentDescription`, `hasSetTextAction`). `testTag` добавляйте
   только если семантический матчер получается слишком сложным.
5. Зависимости от сети/БД заменяйте fake-реализациями из
   `app/src/test/java/com/example/fishing/testutil`.
