# Android Performance Expert Skill

Expertise in optimizing app responsiveness, startup time, and UI smoothness.

## 🧠 Core Concepts
- **Baseline Profiles:** Use Ahead-of-Time (AOT) compilation to supercharge critical user journeys (startup, scrolling).
- **Compiler Metrics:** Generate `composables.txt` reports to identify and fix "unskippable" or "unstable" composables.
- **R8/ProGuard:** Master build-time shrinking and obfuscation to reduce APK size and improve runtime performance.

## 🛠 Rules
1. **Deferred State Reads:** Always prefer lambda versions of modifiers (e.g., `Modifier.offset { ... }`) to skip unnecessary recomposition/layout phases.
2. **Heavy Objects:** Never create heavy objects (Formatters, Lists) inside the body of a Composable. Use `remember` or Move them to the ViewModel.
3. **LazyList Optimization:** Use `key` in `items()` and avoid complex logic inside item builders.
4. **Memory Management:** Monitor for memory leaks using LeakCanary and avoid static context references.
