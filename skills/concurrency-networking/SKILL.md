# Concurrency & Networking Expert Skill

Expertise in asynchronous programming and data fetching in Android.

## 🧠 Core Concepts
- **Kotlin Coroutines:** Master Structured Concurrency, Scopes, and proper use of Dispatchers.
- **Flow & State Management:** Transform cold streams in the Data layer into hot StateFlows in the ViewModel.
- **Offline-First:** Implement robust caching and synchronization strategies between local databases (Room) and network APIs.

## 🛠 Rules
1. **Dispatcher Injection:** Always inject `CoroutineDispatcher` to simplify unit testing.
2. **Handle Exceptions:** Use `CoroutineExceptionHandler` or `runCatching` to prevent app crashes during network failures.
3. **Structured Concurrency:** Always launch coroutines in a managed scope (e.g., `viewModelScope`).
4. **Flow Operators:** Use `collectAsStateWithLifecycle` to consume flows safely in Compose.
