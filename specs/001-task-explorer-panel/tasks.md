# Tasks: Task Explorer Panel

**Input**: Design documents from `/specs/001-task-explorer-panel/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md

**Tests**: Not requested — no test tasks included.

**Organization**: Tasks grouped by user story. US1+US2 form the MVP.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Register extension point and create factory skeleton

- [x] T001 Register `<toolWindow>` extension point in `src/main/resources/META-INF/plugin.xml` with id="Nadle", anchor="right", nadle icon, and factoryClass pointing to `NadleToolWindowFactory`
- [x] T002 [P] Create `NadleToolWindowFactory.kt` in `src/main/kotlin/com/github/nadlejs/intellij/plugin/toolwindow/` implementing `ToolWindowFactory` with `createToolWindowContent()` that delegates to `NadleToolWindowPanel` and `isApplicable()` that returns true only when nadle config files exist in the project

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Extract shared task execution utility needed by US2, US3, and existing SearchEverywhere

- [x] T003 Create `NadleTaskRunner.kt` in `src/main/kotlin/com/github/nadlejs/intellij/plugin/run/` as an `object` with `run(project, task)`, `debug(project, task)`, and `createRunConfiguration(project, task)` methods — extract the find-or-create-config + execute logic from `NadleTaskSearchEverywhereContributor.processSelectedItem()`
- [x] T004 Refactor `NadleTaskSearchEverywhereContributor.kt` in `src/main/kotlin/com/github/nadlejs/intellij/plugin/search/` to delegate `processSelectedItem()` to `NadleTaskRunner.run()` instead of inline execution logic

**Checkpoint**: Foundation ready — `./gradlew compileKotlin` must pass. SearchEverywhere still works as before.

---

## Phase 3: User Story 1 — Discover All Tasks (Priority: P1) MVP

**Goal**: Users open the Nadle tool window and see all tasks grouped by config file in a tree.

**Independent Test**: Open a project with `nadle.config.*` files → open Nadle tool window → verify tree shows config files as parents with task children. Verify empty state for projects without config files.

### Implementation for User Story 1

- [x] T005 [US1] Create `NadleToolWindowPanel.kt` in `src/main/kotlin/com/github/nadlejs/intellij/plugin/toolwindow/` with: (1) a `com.intellij.ui.treeStructure.Tree` backed by `DefaultTreeModel` with invisible root, config file parent nodes showing relative path, and task child nodes showing name + nadle icon; (2) a `loadTasks()` method that runs `NadleTaskScanner.scanTasksDetailed()` on a pooled thread, groups results by `configFilePath`, and updates the tree model on EDT; (3) an empty state label ("No nadle configuration files found") shown when no tasks are discovered; (4) wrap the tree in a `JBScrollPane` and add as panel content
- [x] T006 [US1] Wire `NadleToolWindowFactory.createToolWindowContent()` to instantiate `NadleToolWindowPanel`, add it to the tool window's `ContentManager`, and trigger initial `loadTasks()` on panel creation

**Checkpoint**: Tool window shows task tree. Verify with `./gradlew runIde`.

---

## Phase 4: User Story 2 — Run a Task from the Panel (Priority: P1) MVP

**Goal**: Users double-click or press Enter on a task node to execute it.

**Independent Test**: Double-click a task in the tool window → task runs and output appears in Run tool window. Verify correct working directory for nested config files.

### Implementation for User Story 2

- [x] T007 [US2] Add double-click and Enter key handler to the tree in `NadleToolWindowPanel.kt` — on task node activation, extract the `NadleTask` user object and call `NadleTaskRunner.run(project, task)`

**Checkpoint**: MVP complete — users can discover and run tasks. Verify with `./gradlew runIde`.

---

## Phase 5: User Story 3 — Context Menu Actions (Priority: P2)

**Goal**: Right-click context menu on task nodes with Run, Debug, and Create Run Configuration.

**Independent Test**: Right-click a task → context menu appears → verify Run executes, Debug attaches debugger, Create Run Configuration adds a persistent config.

### Implementation for User Story 3

- [x] T008 [US3] Add right-click popup menu to task nodes in `NadleToolWindowPanel.kt` with three actions: "Run" calling `NadleTaskRunner.run()`, "Debug" calling `NadleTaskRunner.debug()`, and "Create Run Configuration" calling `NadleTaskRunner.createRunConfiguration()` then opening the Run/Debug Configurations dialog

**Checkpoint**: Context menu works. Verify with `./gradlew runIde`.

---

## Phase 6: User Story 4 — Task State Indicators (Priority: P2)

**Goal**: Task nodes show pass/fail/unknown icons based on last execution result.

**Independent Test**: Run a passing task and a failing task → verify green and red icons appear on the respective task nodes. Unrun tasks show default icon.

### Implementation for User Story 4

- [x] T009 [US4] Add a custom `TreeCellRenderer` to the tree in `NadleToolWindowPanel.kt` that queries `NadleTaskStateService.getInstance(project).getResult(filePath, taskName)` for each task node and sets the icon to green (PASSED), red (FAILED), or default nadle icon (null/not run)
- [x] T010 [US4] Subscribe to `ExecutionManager.EXECUTION_TOPIC` in `NadleToolWindowPanel.kt` to detect when a nadle task finishes, then repaint the tree to update state icons in real time

**Checkpoint**: State icons update after task execution. Verify with `./gradlew runIde`.

---

## Phase 7: User Story 5 — Auto-Refresh and Manual Refresh (Priority: P2)

**Goal**: Tree auto-refreshes when config files change. Manual Refresh button in toolbar.

**Independent Test**: Add a `tasks.register()` call to a config file and save → tree updates. Click Refresh button → tree rebuilds.

### Implementation for User Story 5

- [x] T011 [US5] Subscribe to `VirtualFileManager.VFS_CHANGES` via `BulkFileListener` in `NadleToolWindowPanel.kt` — filter events for nadle config files (using `NadleFileUtil.isNadleConfigFile()`) including create, modify, and delete events, then trigger `loadTasks()` to rebuild the tree
- [x] T012 [US5] Add a Refresh toolbar action to the tool window toolbar in `NadleToolWindowPanel.kt` that calls `loadTasks()` when clicked — use `ActionManager` to create an `ActionToolbar` attached to the panel

**Checkpoint**: Auto-refresh and manual refresh work. Verify with `./gradlew runIde`.

---

## Phase 8: User Story 6 — Toolbar Actions (Priority: P3)

**Goal**: Expand All and Collapse All buttons in the toolbar.

**Independent Test**: Click Expand All → all config file nodes expand. Click Collapse All → all collapse.

### Implementation for User Story 6

- [x] T013 [US6] Add Expand All and Collapse All actions to the toolbar in `NadleToolWindowPanel.kt` — Expand All iterates tree rows and expands each, Collapse All iterates in reverse and collapses each. Use `CommonActionsManager.getInstance().createExpandAllAction()` and `createCollapseAllAction()` if available, otherwise implement manually with `TreeUtil`

**Checkpoint**: Toolbar complete. Verify with `./gradlew runIde`.

---

## Phase 9: Polish & Cross-Cutting Concerns

**Purpose**: Final validation and cleanup

- [x] T014 Run `./gradlew compileKotlin` to verify full compilation passes
- [x] T015 Run `./gradlew runIde` and execute quickstart.md validation steps end-to-end

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 (plugin.xml registration)
- **US1 (Phase 3)**: Depends on Phase 1+2 (factory and runner ready)
- **US2 (Phase 4)**: Depends on US1 (tree must exist to add handlers)
- **US3 (Phase 5)**: Depends on US2 (same file, builds on tree interaction pattern)
- **US4 (Phase 6)**: Depends on US1 (tree must exist to add renderer)
- **US5 (Phase 7)**: Depends on US1 (tree must exist to refresh)
- **US6 (Phase 8)**: Depends on US5 (adds to same toolbar created in US5)
- **Polish (Phase 9)**: Depends on all desired user stories

### User Story Dependencies

- **US1 (P1)**: After Phase 2 — no story dependencies
- **US2 (P1)**: After US1 — builds on tree to add run behavior
- **US3 (P2)**: After Phase 2 — can start after US1 (adds context menu to tree)
- **US4 (P2)**: After US1 — can start after US1 (adds icons to tree)
- **US5 (P2)**: After US1 — can start after US1 (adds refresh to tree)
- **US6 (P3)**: After US5 — extends the toolbar

### Parallel Opportunities

After Phase 2 completes:
- T001 and T002 can run in parallel (different files)
- After US1+US2 (MVP), stories US3, US4, and US5 can potentially proceed in parallel as they modify different aspects of NadleToolWindowPanel.kt (context menu, cell renderer, event subscriptions)

### Within Each User Story

- Core implementation before integration
- Story complete and verified before moving to next priority

---

## Parallel Example: Post-MVP Stories

```text
# After MVP (US1 + US2) is complete, these can proceed:

# US3: Context menu (popup menu code)
Task T008: "Add right-click popup menu to NadleToolWindowPanel.kt"

# US4: State icons (cell renderer code)
Task T009: "Add custom TreeCellRenderer to NadleToolWindowPanel.kt"
Task T010: "Subscribe to ExecutionManager.EXECUTION_TOPIC"

# US5: Refresh (event subscription + toolbar code)
Task T011: "Subscribe to VFS_CHANGES in NadleToolWindowPanel.kt"
Task T012: "Add Refresh toolbar action"
```

---

## Implementation Strategy

### MVP First (US1 + US2)

1. Complete Phase 1: Setup (T001–T002)
2. Complete Phase 2: Foundational (T003–T004)
3. Complete Phase 3: US1 — Tree display (T005–T006)
4. Complete Phase 4: US2 — Run tasks (T007)
5. **STOP and VALIDATE**: `./gradlew runIde` — verify task discovery and execution
6. Deploy/demo if ready

### Incremental Delivery

1. Setup + Foundational → Infrastructure ready
2. US1 + US2 → **MVP!** Discovery + Run
3. US3 → Debug + Create Run Config
4. US4 → Visual state feedback
5. US5 → Auto-refresh + manual refresh
6. US6 → Expand/Collapse convenience
7. Each story adds value without breaking previous stories

---

## Notes

- All new files go in `src/main/kotlin/com/github/nadlejs/intellij/plugin/toolwindow/` (except `NadleTaskRunner.kt` which goes in `run/`)
- `NadleToolWindowPanel.kt` is incrementally built across US1–US6 — each story adds new behavior to the same class
- Reuse existing: `NadleTaskScanner`, `NadleTaskStateService`, `NadleFileUtil`, `NadleIcons`, `NadleTaskRunConfiguration`, `NadleTaskConfigurationType`
- Constitution compliance: Platform-First (ToolWindow extension point), Guard Pattern (isApplicable), Regex-Based Discovery (NadleTaskScanner), Simplicity (DefaultTreeModel)
