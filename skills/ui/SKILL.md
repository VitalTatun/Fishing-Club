# Android UI Expert Skill

Expertise in declarative UI construction and system integration.

## 🧠 Core Concepts
- **Edge-to-Edge:** Implement modern system UI layouts using `enableEdgeToEdge()` and proper handling of Insets (WindowInsets).
- **Navigation:** Transition to Navigation Compose with type-safe routing and deep link support.
- **Design Systems:** Map Figma design tokens to Material Design 3 semantic components and color schemes.

## 🛠 Rules
1. **Window Insets:** Always use `Modifier.windowInsetsPadding` or `Modifier.safeDrawingPadding` to avoid overlap with system bars.
2. **Material 3 Tokens:** Use `MaterialTheme.colorScheme` and `MaterialTheme.typography` instead of hardcoded values.
3. **Accessibility:** Provide `contentDescription` for all non-decorative images and ensure touch targets are at least 48dp.
4. **Adaptive Layouts:** Use `WindowSizeClass` to support different screen sizes (phones, tablets, foldables).
