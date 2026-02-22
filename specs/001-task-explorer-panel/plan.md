# Implementation Plan: Task Explorer Panel

**Branch**: `001-task-explorer-panel` | **Date**: 2026-02-22 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-task-explorer-panel/spec.md`

## Summary

Add a dedicated Nadle tool window (sidebar panel) that shows all nadle tasks across the project in a tree view grouped by config file. Users can discover, run, and debug tasks without opening config files. Reuses existing `NadleTaskScanner`, `NadleTaskStateService`, and run configuration infrastructure.

## Technical Context

**Language/Version**: Kotlin 2.1.20, JVM 21
**Primary Dependencies**: IntelliJ Platform SDK 2024.2.5, JavaScript plugin
**Storage**: N/A (all data derived at runtime from filesystem scan and in-memory state)
**Testing**: `./gradlew compileKotlin` + manual verification via `./gradlew runIde`
**Target Platform**: IntelliJ IDEA 2024.2–2025.3 (build range 242–253.*)
**Project Type**: Single IntelliJ plugin project
**Performance Goals**: Tree populates within 2 seconds for projects with up to 50 config files
**Constraints**: Must not block the UI thread during scanning; must respect standard directory exclusions
**Scale/Scope**: Monorepo support — multiple config files across nested directories

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Platform-First | PASS | Uses `<toolWindow>` extension point, `Tree` component, standard toolbar actions |
| II. Guard Pattern | PASS | Tool window only activates for projects with nadle config files |
| III. Compilation Gate | PASS | `./gradlew compileKotlin` required before commit |
| IV. Regex-Based Task Discovery | PASS | Reuses `NadleTaskScanner` which uses `NadleFileUtil.TASK_REGISTER_PATTERN` |
| V. Explicit Imports | PASS | No wildcard imports |
| VI. Package Organization | PASS | New `toolwindow/` subpackage (3+ classes) |
| VII. Manual Verification | PASS | Verification steps in quickstart.md |
| VIII. Simplicity | PASS | Two-level tree with `DefaultTreeModel`, no abstraction layers |

**Post-design re-check**: All gates still pass. No violations.

## Project Structure

### Documentation (this feature)

```text
specs/001-task-explorer-panel/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
└── tasks.md             # Phase 2 output (created by /speckit.tasks)
```

### Source Code (repository root)

```text
src/main/kotlin/com/github/nadlejs/intellij/plugin/
├── toolwindow/                          # NEW package
│   ├── NadleToolWindowFactory.kt        # ToolWindowFactory extension point
│   └── NadleToolWindowPanel.kt          # Tree UI, actions, refresh logic
├── run/
│   ├── NadleTaskRunner.kt               # NEW: shared execution utility
│   └── (existing files unchanged)
└── (other packages unchanged)

src/main/resources/
└── META-INF/plugin.xml                  # MODIFIED: add <toolWindow> registration
```

**Structure Decision**: Single project, new `toolwindow/` subpackage with 2 classes. One new utility class (`NadleTaskRunner`) in existing `run/` package to extract shared execution logic from `NadleTaskSearchEverywhereContributor`.

## Design Details

### New Files

#### 1. `NadleToolWindowFactory.kt`

Implements `ToolWindowFactory`. Registered via `<toolWindow>` in plugin.xml.

- `createToolWindowContent(project, toolWindow)`: Creates `NadleToolWindowPanel` and adds it as the tool window's content component.
- `isApplicable(project)`: Returns `true` only if the project contains at least one nadle config file (lazy check — scan on first open, not on project load).

#### 2. `NadleToolWindowPanel.kt`

Main panel class containing the tree, toolbar, and event subscriptions.

**Tree Structure**:
- Root (invisible) → Config file nodes → Task nodes
- Uses `DefaultTreeModel` with `DefaultMutableTreeNode`
- Config file nodes display relative path from project root
- Task nodes display task name + state icon

**Toolbar Actions**:
- Run (selected task)
- Debug (selected task)
- Refresh
- Expand All / Collapse All

**Context Menu** (on task nodes):
- Run
- Debug
- Create Run Configuration

**Event Subscriptions** (via project `MessageBus`):
- `VirtualFileManager.VFS_CHANGES` → auto-refresh tree on config file changes
- `ExecutionManager.EXECUTION_TOPIC` → update task state icons after execution

**Background Scanning**:
- `loadTasks()`: Runs `NadleTaskScanner.scanTasksDetailed()` on a pooled thread, groups results by `configFilePath`, then updates tree model on EDT.

#### 3. `NadleTaskRunner.kt`

Shared utility extracted from `NadleTaskSearchEverywhereContributor.processSelectedItem()`.

```kotlin
object NadleTaskRunner {
    fun run(project: Project, task: NadleTask)
    fun debug(project: Project, task: NadleTask)
    fun createRunConfiguration(project: Project, task: NadleTask): RunnerAndConfigurationSettings
}
```

- `run()`: Finds or creates a run configuration, then executes with `DefaultRunExecutor`
- `debug()`: Same but uses `DefaultDebugExecutor`
- `createRunConfiguration()`: Creates a persistent (non-temporary) run configuration

### Modified Files

#### 4. `plugin.xml`

Add `<toolWindow>` registration:
```xml
<toolWindow id="Nadle"
            anchor="right"
            icon="com.github.nadlejs.intellij.plugin.util.NadleIcons.Nadle"
            factoryClass="com.github.nadlejs.intellij.plugin.toolwindow.NadleToolWindowFactory"/>
```

#### 5. `NadleTaskSearchEverywhereContributor.kt`

Refactor `processSelectedItem()` to delegate to `NadleTaskRunner.run()` instead of inline execution logic.

### Reused Components

| Component | Location | Usage |
|-----------|----------|-------|
| `NadleTaskScanner.scanTasksDetailed()` | `run/NadleTaskScanner.kt` | Populate task tree |
| `NadleTaskStateService.getInstance()` | `run/NadleTaskStateService.kt` | Task pass/fail icons |
| `NadleFileUtil.isNadleConfigFile()` | `util/NadleFileUtil.kt` | Filter VFS events |
| `NadleIcons.Nadle` | `util/NadleIcons.kt` | Tool window icon, task node icon |
| `NadleTaskRunConfiguration` | `run/NadleTaskRunConfiguration.kt` | Create run configs |
| `NadleTaskConfigurationType` | `run/NadleTaskConfigurationType.kt` | Find config type |
| VFS change listener pattern | `run/NadleTaskExecutionListener.kt` | Auto-refresh on file changes |
| Execution listener pattern | `run/NadleTaskExecutionListener.kt` | Update state icons |
