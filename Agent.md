# Fishing Project Architecture & Agent Skills

This project follows the modern Android development practices outlined in the [Awesome Android Agent Skills](https://github.com/new-silvermoon/awesome-android-agent-skills) repository.

## 🧠 Active Agent Skills
I have the following expert skills installed and active in this project:

1.  **[Jetpack Compose Expert](skills/compose-expert/SKILL.md)**: High-performance UI, Modifier order, Stability, and Material 3.
2.  **[Android Architecture Expert](skills/architecture/SKILL.md)**: Clean Architecture, MVVM, UDF, and Offline-first patterns.
3.  **[Performance Expert](skills/performance/SKILL.md)**: Audit, Recomposition optimization, and fast build logic.
4.  **[Testing & Automation Expert](skills/testing/SKILL.md)**: Unit tests, UI tests, and Robot pattern.
5.  **[Android Gradle Logic Expert](skills/build-logic/SKILL.md)**: Convention Plugins, Version Catalogs, and build optimization.
6.  **[Concurrency & Networking Expert](skills/concurrency-networking/SKILL.md)**: Coroutines, Flow, Retrofit, and Dispatcher injection.
7.  **[Migration Expert](skills/migration/SKILL.md)**: XML to Compose, RxJava to Coroutines, and Room migrations.
8.  **[General UI Expert](skills/ui/SKILL.md)**: Accessibility, Coil, Navigation, and Adaptive UI.

## 🏗 Project Architecture
- **Pattern:** MVVM + Clean Architecture principles.
- **UI:** 100% Jetpack Compose with Material 3.
- **Dependency Injection:** Hilt (Planned/Active).
- **Asynchronous Work:** Kotlin Coroutines & Flow.
- **Data Layer:** Room (Local Database) + Repository Pattern (Offline-first).
- **Navigation:** Type-safe Compose Navigation.

## 📁 Project Structure
- `com.example.fishing.model`: Domain models.
- `com.example.fishing.ui.navigation`: Routes and NavGraph.
- `com.example.fishing.ui.screens`: Screen-level composables.
- `com.example.fishing.ui.viewmodel`: ViewModels.
- `com.example.fishing.ui.components`: Reusable UI elements.
- `com.example.fishing.data`: Repositories and Local DB.
