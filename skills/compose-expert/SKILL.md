# Jetpack Compose Expert Skill

Expert guidance for high-performance Jetpack Compose development.

## 🧠 Core Concepts
- **Stability & Snapshot System:** Understand the contract between code and the Compose compiler (using `@Stable`, `@Immutable`).
- **Positional Memoization:** Master the use of `remember` and `derivedStateOf` to eliminate redundant computations.
- **Advanced UI:** Construction of custom layouts and complex state hoisting patterns.

## 🛠 Rules
1. **Recomposition Optimization:** Ensure recomposition remains cheap by avoiding allocations in bodies and using stable types.
2. **Modifier Order:** Follow the "Layout-then-Action" rule. Sequence significantly affects the visual and functional outcome.
3. **State Management:** Keep state as low as possible. Use `derivedStateOf` for values that depend on other state.
4. **Side Effects:** Always use the correct effect API (`LaunchedEffect`, `DisposableEffect`) with stable and relevant keys.
5. **Modern Navigation:** Use Navigation 2.8.0+ type-safe approach with Kotlin Serialization.
