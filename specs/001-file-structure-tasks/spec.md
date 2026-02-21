# Feature Specification: File Structure Popup Shows Nadle Tasks

**Feature Branch**: `001-file-structure-tasks`
**Created**: 2026-02-21
**Status**: Draft
**Input**: User description: "now I want to popup window File Structure in nadle config file show tasks"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View All Tasks via File Structure (Priority: P1)

A developer opens a nadle config file and wants to quickly see all registered tasks. They invoke File Structure (Cmd+F12 / Ctrl+F12) and see a popup listing every task defined via `tasks.register()` in the current file. They can click or press Enter on a task to navigate directly to its definition in the file.

**Why this priority**: This is the core feature. Without task listing in File Structure, the feature has no value.

**Independent Test**: Can be fully tested by opening any nadle config file, pressing Cmd+F12, and verifying tasks appear in the popup. Delivers immediate value by enabling fast task discovery and navigation.

**Acceptance Scenarios**:

1. **Given** a nadle config file with 3 registered tasks, **When** the user invokes File Structure (Cmd+F12), **Then** all 3 tasks are listed by name in the popup
2. **Given** a nadle config file with no registered tasks, **When** the user invokes File Structure, **Then** the popup shows an empty state (no tasks listed)
3. **Given** the File Structure popup is open with tasks listed, **When** the user selects a task and presses Enter, **Then** the editor navigates to the line where that task is defined

---

### User Story 2 - Filter Tasks by Typing (Priority: P2)

With the File Structure popup open, the developer starts typing to filter the task list. Only tasks whose names match the typed text are shown. This allows quick navigation in files with many tasks.

**Why this priority**: Builds on P1 to make the feature practical for files with many tasks. The File Structure popup has built-in type-to-filter behavior, so this should work automatically once tasks are listed.

**Independent Test**: Can be tested by opening File Structure in a config file with multiple tasks, typing a partial task name, and verifying the list filters down.

**Acceptance Scenarios**:

1. **Given** the File Structure popup shows 10 tasks, **When** the user types "build", **Then** only tasks containing "build" in their name are shown
2. **Given** the user is filtering tasks, **When** they clear the filter text, **Then** all tasks are shown again

---

### Edge Cases

- What happens when the file has syntax errors? Tasks that can still be pattern-matched should still appear.
- What happens when a task name is duplicated? Both entries should appear with their respective line locations.
- What happens in a non-nadle config file? File Structure should behave normally (default behavior for that file type).

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The File Structure popup MUST list all tasks registered via `tasks.register()` in the current nadle config file
- **FR-002**: Each task entry MUST display the task name as its primary label
- **FR-003**: Each task entry MUST display the Nadle icon to visually distinguish tasks from other file elements
- **FR-004**: Selecting a task entry MUST navigate the editor cursor to the line where that task is defined in the file
- **FR-005**: The File Structure popup MUST support type-to-filter for task names
- **FR-006**: The File Structure popup MUST only show task entries for nadle config files (files matching `nadle.config.[cm]?[jt]s`)
- **FR-007**: The feature MUST NOT interfere with the default File Structure behavior for non-nadle files

### Assumptions

- The File Structure popup is IntelliJ's built-in "File Structure" feature (Cmd+F12 / Ctrl+F12), not a custom popup
- Task discovery uses the existing `tasks.register('taskName', ...)` pattern matching already present in the codebase
- Type-to-filter is provided by the IntelliJ File Structure framework and does not require custom implementation

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: All `tasks.register()` calls in a nadle config file appear in the File Structure popup within 1 second of invocation
- **SC-002**: Selecting a task from the popup navigates to the correct source line 100% of the time
- **SC-003**: Type-to-filter narrows results correctly for partial task name matches
- **SC-004**: Non-nadle files are unaffected — their File Structure behavior remains unchanged
