# Fishing Project Architecture & Agents

This project follows the modern Android development practices outlined in the [Awesome Android Agent Skills](https://github.com/new-silvermoon/awesome-android-agent-skills) repository.

## 🤖 Agents

The team is defined in `.opencode/agents/` (OpenCode V2 agent files). Each agent has
its own prompt, its own permission set and, optionally, its own model. Permissions
are enforced by the runtime, not by the prompt: an agent without `edit` rights
cannot modify files even if it tries.

| Agent | Mode | Role | Writes files |
| --- | --- | --- | --- |
| `orchestrator` | primary | Plans the work, routes it, aggregates the reports | No (read + git inspect only) |
| `analysis` | subagent | Turns a request into a spec: goal, acceptance criteria, states, edge cases, out of scope | Only `specs/` |
| `architect` | all | Designs the solution: layers, components, data flow, DI, risks, implementation order | Only `docs/` and `specs/` |
| `developer` | subagent | Implements code changes and fixes build errors | Yes |
| `tester` | subagent | Writes tests and runs `./gradlew :app:testDebugUnitTest` | Only `app/src/test`, `app/src/androidTest` |
| `reviewer` | subagent | Read-only review of the diff, findings by severity, final verdict | No |

### Workflow

`orchestrator` picks the lightest path that still de-risks the task:

- Small fix: `developer` → `tester`.
- New feature or a data-layer change: `analysis` → `architect` → `developer` →
  `tester` → `reviewer`.
- Review only: `reviewer` against the current diff.

Reviewer findings go back to `developer`, then `tester` re-runs. The loop ends at
`APPROVE` / `APPROVE WITH NITS` (or when the rest is explicitly out of scope).

Subagents run in child sessions with a fresh context: whatever the orchestrator
needs them to know must be in the task it hands over. Call a single one directly
when you need just that step, e.g. "use the reviewer subagent on my current
changes".

Every agent checks the available skill list first and loads the matching skill
with the `skill` tool. If nothing fits, it says so and continues without one.

### Slash commands

`default_agent` is `build`, so ordinary work starts there. The team is switched
on per task from `.opencode/commands/`:

| Command | Agent | Effect |
| --- | --- | --- |
| `/team <task>` | `orchestrator` | Switches the current session to the orchestrator and hands it the task. |
| `/spec <request>` | `analysis` | Runs the analyst in a child session and returns a specification. |
| `/code-review [scope]` | `reviewer` | Runs the read-only review of the working diff in a child session. |

The built-in `/review` is left untouched. Since it is a subagent command, the
parent keeps its agent: on `build` you can use the roles without losing your
place.

## 🧠 Skills

Project skills live in `skills/` and are registered through `"skills": ["./skills"]`
in `opencode.json`. Under that path OpenCode discovers every directory with a
`SKILL.md`, so a new skill needs nothing but a folder and a `description` in its
frontmatter - without a description the model never sees it. Global skills from
`~/.config/opencode/skills` load too; on an ID collision the project copy wins.

The folder contains only the official Android skills from
[android/skills](https://github.com/android/skills.git). Typical ones:

- `adaptive`, `edge-to-edge` - window size classes, multi-pane and insets;
- `jetpack-compose-performance`, `android-profiler`, `r8-analyzer` - performance;
- `testing-setup` - test harness and Compose UI tests;
- `navigation-3`, `navigation-event` - navigation and back handling;
- `agp-9-upgrade` - Gradle/AGP build logic;
- `android-*-security`, `anr-audit-and-fix`, `play-policy-insights` and others.

Review `skills/` before assuming a topic is uncovered; do not invent a skill ID.
Keep the folder in sync with the upstream repository (`android skills update --all`).

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

## 🧪 Testing
- Run every unit and Compose UI test locally (no emulator, no Supabase):
  `./gradlew :app:testDebugUnitTest`
- Compose UI tests live in `app/src/test` and run through Robolectric with
  `createComposeRule()`; screens are driven by fakes from
  `app/src/test/java/com/example/fishing/testutil`.
- Current UI suites: `LoginScreenUiTest`, `CreateReportScreenUiTest`,
  `ReportDetailScreenUiTest`.
- HTML report: `app/build/reports/tests/testDebugUnitTest/index.html`.
- Full testing strategy, CI command and guidance for new tests:
  [docs/testing.md](docs/testing.md).
