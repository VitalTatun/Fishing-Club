# Android Migration Expert Skill

Expertise in gradual modernization and refactoring of Android applications.

## 🧠 Core Concepts
- **XML to Compose:** Strategies for interoperability using `AbstractComposeView` and gradual screen-by-screen migration.
- **Incremental Refactoring:** Maintain a shippable product while replacing View-system dependencies with Compose and modern architecture.
- **Component Upgrades:** Documentation for upgrading critical libraries like Play Billing and AGP 9.

## 🛠 Rules
1. **Interoperability:** Use `AndroidView` or `ComposeView` for gradual migration without breaking existing functionality.
2. **Room Migrations:** Always provide automated and manual test paths for database migrations.
3. **Third-party Libraries:** Evaluate and replace legacy libraries (e.g., Glide with Coil, Retrofit with Ktor if applicable).
4. **Binary Compatibility:** Ensure changes don't break existing data structures or serialized objects.
