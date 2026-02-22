# Feature Specification: Tool Window — Task Explorer Panel

**Feature Branch**: `001-task-explorer-panel`
**Created**: 2026-02-22
**Status**: Draft
**Input**: GitHub issue #29 — "Tool Window: Task Explorer panel"

## User Scenarios & Testing *(mandatory)*

### User Story 1 — Discover All Tasks (Priority: P1)

A developer opens a nadle project (or monorepo) in IntelliJ and wants to see every available task without manually opening config files. They click the "Nadle" tool window in the sidebar and immediately see a tree of all tasks grouped by config file.

**Why this priority**: Task discovery is the core value proposition. Without it, users resort to opening files and scanning gutter icons — a slow, error-prone workflow. This is the minimum viable product.

**Independent Test**: Open a project with one or more `nadle.config.*` files, open the Nadle tool window, and verify all registered tasks appear in a tree grouped by config file.

**Acceptance Scenarios**:

1. **Given** a project with a single `nadle.config.ts` containing 5 registered tasks, **When** the user opens the Nadle tool window, **Then** a tree shows the config file as a parent node with 5 task child nodes beneath it.
2. **Given** a monorepo with `nadle.config.ts` at the root and `packages/app/nadle.config.ts`, **When** the user opens the Nadle tool window, **Then** both config files appear as separate parent nodes, each showing their respective tasks.
3. **Given** a project with no `nadle.config.*` files, **When** the user opens the Nadle tool window, **Then** an empty state message is displayed (e.g., "No nadle configuration files found").

---

### User Story 2 — Run a Task from the Panel (Priority: P1)

A developer sees a task in the tool window and wants to execute it immediately. They double-click the task (or press Enter) and the task runs in the Run tool window using the standard run configuration.

**Why this priority**: Running tasks is the primary action after discovery. Together with Story 1 this forms the complete MVP — see tasks and run them.

**Independent Test**: Double-click any task in the tool window and verify it launches a run configuration that executes the task and shows output in the Run tool window.

**Acceptance Scenarios**:

1. **Given** the Nadle tool window showing tasks, **When** the user double-clicks a task, **Then** the task executes and output appears in the Run tool window.
2. **Given** a task in a subdirectory config file, **When** the user runs it, **Then** the working directory is set to that config file's parent directory.
3. **Given** a task that was previously run and failed, **When** the user double-clicks it again, **Then** it re-runs with a fresh execution.

---

### User Story 3 — Context Menu Actions (Priority: P2)

A developer right-clicks a task in the tool window to access additional actions beyond the default run: Debug and Create Run Configuration.

**Why this priority**: Debug is essential for troubleshooting failing tasks, and creating a persistent run configuration allows users to customize execution. These enhance the core run experience but are not required for MVP.

**Independent Test**: Right-click a task and verify the context menu shows Run, Debug, and Create Run Configuration. Select each and verify correct behavior.

**Acceptance Scenarios**:

1. **Given** a task in the tool window, **When** the user right-clicks it, **Then** a context menu appears with "Run", "Debug", and "Create Run Configuration" actions.
2. **Given** the user selects "Debug" from the context menu, **When** the task starts, **Then** it runs in debug mode with the debugger attached.
3. **Given** the user selects "Create Run Configuration", **When** the action completes, **Then** a new run configuration for that task appears in the Run/Debug Configurations dialog.

---

### User Story 4 — Task State Indicators (Priority: P2)

After running tasks, the developer wants visual feedback in the tool window about which tasks passed and which failed, so they can quickly identify problems.

**Why this priority**: State indicators provide at-a-glance feedback and reduce context switching. They complement the run workflow but are not blocking for basic task execution.

**Independent Test**: Run a passing task and a failing task, then verify the tool window shows distinct visual indicators (icons/colors) for each state.

**Acceptance Scenarios**:

1. **Given** a task that completed successfully, **When** the user views the tool window, **Then** the task shows a pass indicator (green icon).
2. **Given** a task that failed, **When** the user views the tool window, **Then** the task shows a failure indicator (red icon).
3. **Given** a task that has never been run, **When** the user views the tool window, **Then** the task shows the default nadle icon with no state indicator.

---

### User Story 5 — Auto-Refresh and Manual Refresh (Priority: P2)

A developer adds or removes tasks in a config file and expects the tool window to reflect changes without manual intervention. They can also click a refresh button to force a rescan.

**Why this priority**: Keeping the task list in sync is important for trust in the tool, but a manual refresh button is an acceptable fallback for the initial release.

**Independent Test**: Edit a config file to add a new task registration, then verify the tool window updates. Also test the manual refresh button.

**Acceptance Scenarios**:

1. **Given** the tool window is open, **When** the user adds a new `tasks.register()` call to a config file and saves, **Then** the tool window updates to show the new task.
2. **Given** the tool window is open, **When** the user removes a task registration and saves, **Then** the removed task disappears from the tool window.
3. **Given** the tool window is open, **When** the user clicks the Refresh button in the toolbar, **Then** the task tree is rebuilt from a fresh scan.

---

### User Story 6 — Toolbar Actions (Priority: P3)

The tool window toolbar provides convenience actions: Expand All and Collapse All for navigating large task trees.

**Why this priority**: Convenience feature for large monorepos. Not essential for core functionality.

**Independent Test**: With a multi-config-file tree, click Expand All and verify all nodes expand; click Collapse All and verify all nodes collapse.

**Acceptance Scenarios**:

1. **Given** a collapsed tree with multiple config files, **When** the user clicks "Expand All", **Then** all config file nodes expand to show their tasks.
2. **Given** a fully expanded tree, **When** the user clicks "Collapse All", **Then** all config file nodes collapse.

---

### Edge Cases

- What happens when a config file has syntax errors preventing task extraction? The config file node should still appear but with no task children (or an error indicator).
- What happens when a config file is deleted while the tool window is open? The config file node should be removed on the next refresh cycle.
- What happens when the project has hundreds of tasks across many config files? The tree should remain responsive and load without blocking the UI thread.
- What happens when Node.js is not configured? The tool window should still display tasks (task scanning is file-based, not runtime-dependent).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Plugin MUST register a tool window accessible from the IDE sidebar with the nadle icon.
- **FR-002**: Tool window MUST display a tree structure with config files as parent nodes and tasks as child nodes.
- **FR-003**: Config file nodes MUST show the relative path from the project root.
- **FR-004**: Task nodes MUST show the task name and the nadle icon.
- **FR-005**: Plugin MUST scan all `nadle.config.*` files across the project directory tree (respecting standard exclusions: `node_modules`, `.git`, `dist`, `build`, etc.).
- **FR-006**: Users MUST be able to run a task by double-clicking or pressing Enter on a task node.
- **FR-007**: Task execution MUST use the correct working directory (the parent directory of the task's config file).
- **FR-008**: Users MUST be able to right-click a task to access Run, Debug, and Create Run Configuration actions.
- **FR-009**: Tool window MUST display pass/fail state indicators on task nodes that have been previously executed.
- **FR-010**: Tool window MUST auto-refresh when `nadle.config.*` files are created, modified, or deleted.
- **FR-011**: Tool window toolbar MUST include a manual Refresh button.
- **FR-012**: Tool window toolbar MUST include Expand All and Collapse All actions.
- **FR-013**: Task scanning and tree building MUST NOT block the UI thread.

### Key Entities

- **Config File Node**: Represents a `nadle.config.*` file in the project. Displays relative path. Contains zero or more task nodes.
- **Task Node**: Represents a single `tasks.register()` call. Displays task name and execution state. Associated with a config file and working directory.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can discover all tasks across a monorepo from a single panel without opening any config files.
- **SC-002**: Users can run any task within 2 interactions from opening the tool window (open panel → double-click task).
- **SC-003**: The tool window populates its task tree within 2 seconds for projects with up to 50 config files.
- **SC-004**: Task state (pass/fail) is visually distinguishable at a glance without hovering or clicking.
- **SC-005**: The tool window stays in sync with config file changes without requiring IDE restart.

## Assumptions

- Task scanning uses the existing file-based regex approach (not runtime evaluation), which means only literal string task names in `tasks.register("name")` calls are detected.
- The tool window placement defaults to the right sidebar, consistent with Gradle/Maven tool windows.
- Standard directory exclusions (`node_modules`, `.git`, `dist`, `build`, `.next`, `.nuxt`, `coverage`, `.turbo`, `.cache`, `out`) apply during scanning.
- Task state indicators reuse the existing task state tracking mechanism already in the plugin.
