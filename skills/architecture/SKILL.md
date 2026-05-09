# Android Architecture Expert Skill

Expertise in Modern Android Architecture (MVVM/MVI) and Clean Architecture.

## 🧠 Core Principles
- **Layer Separation:** Maintain a clear distinction between UI, Domain, and Data layers.
- **State Machines:** Treat ViewModels as deterministic state machines processing events and emitting a single UI state.
- **Dependency Injection:** Use the API-Impl pattern to minimize inter-module dependencies and improve testability.

## 🛠 Rules
1. **UDF (Unidirectional Data Flow):** Events go up, state comes down.
2. **Offline-First:** Implement robust local caching as the primary source of truth.
3. **Dispatcher Injection:** Always inject `CoroutineDispatcher` to ensure testability.
4. **Repository Pattern:** Encapsulate data logic and handle synchronization between network and database.
