# План реализации отдельного экрана регистрации

Необходимо разделить логику входа и регистрации. Регистрация теперь будет включать дополнительные поля: **Имя пользователя** и **Фотография профиля**.

## User Review Required

> [!IMPORTANT]
> Для загрузки фотографии профиля в Supabase необходимо, чтобы в хранилище (Storage) был создан бакет `avatars` с публичным доступом на чтение.

## Proposed Changes

### Data Layer

#### [MODIFY] [AuthRepository.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/data/AuthRepository.kt)
- Обновить метод `register`, добавив параметры `name: String` и `photoUri: String?`.

#### [MODIFY] [SupabaseAuthRepository.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/data/SupabaseAuthRepository.kt)
- Реализовать загрузку аватара в бакет `avatars` при регистрации.
- Обновить метаданные пользователя в Supabase Auth (`full_name` и `avatar_url`).
- Синхронизировать данные в таблицу `profiles` (если она используется для публичных данных).

---

### UI Layer

#### [NEW] [RegistrationViewModel.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/ui/viewmodel/RegistrationViewModel.kt)
- Создать ViewModel для управления состоянием регистрации:
    - Поля: `name`, `email`, `password`, `photoUri`.
    - Состояния: `isLoading`, `error`, `isRegistered`.
    - Метод `register()`: валидация, копирование фото через `PhotoUtils` и вызов репозитория.

#### [NEW] [RegistrationScreen.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/ui/screens/login/RegistrationScreen.kt)
- Создать экран регистрации:
    - Выбор фото (круглый аватар с иконкой редактирования).
    - Текстовые поля для Имени, Email и Пароля.
    - Кнопка регистрации с индикатором загрузки.
    - Кнопка возврата на экран логина.

#### [MODIFY] [LoginScreen.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/ui/screens/login/LoginScreen.kt)
- Изменить `onRegisterClick` в `LoginContent`, чтобы он выполнял переход на новый экран вместо вызова регистрации.

#### [MODIFY] [FishingNavHost.kt](file:///Users/vital/AndroidStudioProjects/Fishing/app/src/main/java/com/example/fishing/ui/navigation/FishingNavHost.kt)
- Добавить маршрут `"registration"` в NavGraph.
- Настроить внедрение `RegistrationViewModel`.

## Verification Plan

### Manual Verification
1. Открыть приложение, нажать «Создать аккаунт».
2. Проверить переход на новый экран.
3. Выбрать фотографию из галереи.
4. Ввести имя, email и пароль.
5. Нажать «Зарегистрироваться».
6. Убедиться, что после регистрации происходит переход в основную часть приложения, а в профиле отображаются корректное имя и фото.
