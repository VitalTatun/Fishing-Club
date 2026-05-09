# Android Testing Expert Skill

Expertise in comprehensive testing strategies for Android.

## 🧠 Core Concepts
- **Compose Testing APIs:** Transition from `launchFragmentInContainer` to `createAndroidComposeRule` for modern UI testing.
- **Screenshot Testing:** Use dedicated libraries to validate UI appearance across different configurations and locales.
- **Edge Case Testing:** Rigorously test state survival during process death and configuration changes (e.g., rotation).

## 🛠 Rules
1. **Robot Pattern:** Use the Robot pattern to decouple test logic from UI implementation details.
2. **Hilt in Tests:** Use `HiltAndroidRule` to manage dependencies in instrumentation tests.
3. **Hermetic Tests:** Ensure tests are isolated and don't depend on external network or state.
4. **Test Composition:** Test small composables in isolation using `ComposeContentTestRule`.
