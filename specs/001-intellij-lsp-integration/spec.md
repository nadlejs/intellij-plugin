# Feature Specification: IntelliJ LSP Integration for Nadle Language Server

**Feature Branch**: `001-intellij-lsp-integration`
**Created**: 2026-02-21
**Status**: Draft
**Input**: User description: "Rewrite the whole repo or fix current file to support for IntelliJ, the current LSP already exists in ../nadle repo, check it"

## Clarifications

### Session 2026-02-21

- Q: What does "Debug" mean for Nadle tasks? → A: Debug launches the Node.js process with the `--inspect` flag so IntelliJ's built-in Node.js debugger can attach, allowing breakpoints in task handler code (same pattern as Vitest/Mocha debug).
- Q: How should the plugin locate the language server? → A: Project `node_modules` first, then global fallback — zero config for most users. No manual path configuration required.
- Q: When should pass/fail gutter icons reset? → A: Reset on file edit — any change to the file clears all pass/fail icons back to the default play button (▶). Stale results are misleading after code changes.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Real-Time Error Feedback in Nadle Config Files (Priority: P1)

As a developer working on a Nadle project in IntelliJ IDEA, I want to see real-time error highlighting in my `nadle.config.ts` files so that I can catch task definition mistakes (invalid names, duplicates, unresolved dependencies) without leaving the editor.

**Why this priority**: Diagnostics are the most critical LSP feature — they provide immediate, passive value without requiring user action. Every time a developer opens or edits a nadle config file, they automatically benefit from error detection.

**Independent Test**: Can be fully tested by opening a `nadle.config.ts` file with known errors (e.g., invalid task name `"123bad"`, duplicate registrations) and verifying that error/warning markers appear in the editor gutter and Problems panel.

**Acceptance Scenarios**:

1. **Given** a `nadle.config.ts` file with a task name `"123-invalid"` that doesn't match the naming convention, **When** the file is opened in IntelliJ, **Then** the invalid task name is underlined with an error indicator and the message reads: `Task name "123-invalid" is invalid. Names must match /^[a-z]([a-z0-9-]*[a-z0-9])?$/`
2. **Given** a `nadle.config.ts` file with two tasks registered as `"build"`, **When** the file is opened, **Then** the duplicate registration is underlined with an error and the message identifies where the original was registered.
3. **Given** a `nadle.config.ts` file where a task declares `dependsOn: ["nonexistent"]`, **When** the file is opened, **Then** a warning marker appears on `"nonexistent"` indicating the dependency is unresolved.
4. **Given** a valid `nadle.config.ts` file with no errors, **When** the file is opened, **Then** no error or warning markers appear.
5. **Given** an open `nadle.config.ts` file with an error, **When** the developer fixes the error and the file is re-analyzed, **Then** the error marker disappears.

---

### User Story 2 - Task Name Autocompletion in Dependencies (Priority: P2)

As a developer configuring task dependencies, I want autocompletion for task names when typing inside `dependsOn` arrays so that I can quickly reference existing tasks without memorizing names or switching between files.

**Why this priority**: Completions accelerate workflow and reduce errors. They are the second most impactful feature because they actively assist during the authoring phase, building on the passive diagnostics from P1.

**Independent Test**: Can be fully tested by placing the cursor inside a `dependsOn` string literal and triggering autocomplete. Verify the popup shows all registered task names (excluding the current task) with their form and description.

**Acceptance Scenarios**:

1. **Given** a `nadle.config.ts` with tasks `"build"`, `"test"`, and `"lint"` registered, **When** the developer types `dependsOn: ["` inside the `"test"` task's config and triggers completion, **Then** a completion popup appears showing `"build"` and `"lint"` (but not `"test"` itself).
2. **Given** a completion popup is shown, **When** the developer selects a task name, **Then** the selected name is inserted into the string literal.
3. **Given** a `nadle.config.ts` file with a typed task `tasks.register("compile", ExecTask, {...})`, **When** completion is triggered in a `dependsOn`, **Then** the completion item for `"compile"` shows detail text indicating its form (e.g., `"typed (ExecTask)"`).

---

### User Story 3 - Hover Information for Task Names (Priority: P3)

As a developer reading a nadle config file, I want to hover over task names to see their metadata (form, description, dependencies, group, inputs, outputs) so that I can understand task configurations without scrolling to their definitions.

**Why this priority**: Hover provides contextual information on-demand — it's useful for code comprehension, especially in files with many tasks, but is less critical than error detection and autocompletion.

**Independent Test**: Can be fully tested by hovering the cursor over a task name string in either a `tasks.register()` call or a `dependsOn` reference and verifying the popup displays the expected metadata.

**Acceptance Scenarios**:

1. **Given** a task `tasks.register("build", ExecTask, {...}).config({ description: "Compile TypeScript", group: "Building" })`, **When** the developer hovers over `"build"`, **Then** a popup shows the task name, form (`typed: ExecTask`), description, and group.
2. **Given** a task with `dependsOn: ["lint"]` and `"lint"` is defined elsewhere in the file, **When** hovering over `"lint"` inside `dependsOn`, **Then** the popup shows the `"lint"` task's metadata.
3. **Given** a task with no description or group, **When** hovering over its name, **Then** the popup shows only the task name and form without empty sections.

---

### User Story 4 - Navigate to Task Definition from Dependencies (Priority: P4)

As a developer reviewing task dependencies, I want to Ctrl+Click (Cmd+Click on macOS) on a task name inside `dependsOn` to jump to its `tasks.register()` definition so that I can quickly navigate between dependent tasks.

**Why this priority**: Go-to-definition is a convenience feature that enhances navigation in larger config files. It builds on top of the core features but is the least frequently used compared to diagnostics, completions, and hover.

**Independent Test**: Can be fully tested by Ctrl+Clicking on a task name string inside a `dependsOn` array and verifying the cursor jumps to the corresponding `tasks.register()` call.

**Acceptance Scenarios**:

1. **Given** task `"test"` has `dependsOn: ["build"]` and `"build"` is defined at line 5, **When** the developer Ctrl+Clicks on `"build"` inside `dependsOn`, **Then** the cursor navigates to the `tasks.register("build", ...)` call at line 5.
2. **Given** a `dependsOn` reference contains a workspace-qualified name like `"core:build"` (contains `:`), **When** the developer Ctrl+Clicks on it, **Then** no navigation occurs (workspace-qualified references are out of scope).
3. **Given** a `dependsOn` reference to a task name that doesn't exist in the file, **When** the developer Ctrl+Clicks on it, **Then** no navigation occurs.

---

### User Story 5 - Enhanced Task Runner Gutter Experience (Priority: P1)

As a developer using the Nadle IntelliJ plugin, I want a rich gutter play button experience next to each task definition — similar to how Vitest and Mocha plugins show run icons next to tests — with a context menu for Run/Debug, execution status feedback in the gutter, and seamless run configuration management.

**Why this priority**: The gutter play button is the primary interaction point for running tasks. An enhanced experience (context menu, status icons, debug support) brings the plugin to parity with best-in-class IntelliJ test runner plugins and significantly improves developer productivity.

**Independent Test**: Can be fully tested by opening a `nadle.config.ts` file and verifying that each `tasks.register()` line has a play button, right-clicking shows Run/Debug options, clicking runs the task, and after execution the icon updates to reflect pass/fail status.

**Acceptance Scenarios**:

1. **Given** a `nadle.config.ts` file is open, **When** viewing a `tasks.register("build", ...)` line, **Then** a green play button (▶) icon appears in the gutter next to that line.
2. **Given** the play button is visible, **When** the developer left-clicks it, **Then** a context menu appears with options: "Run 'Nadle: build'" and "Debug 'Nadle: build'".
3. **Given** the context menu is shown, **When** the developer selects "Run 'Nadle: build'", **Then** the task executes via `npx nadle build` and output appears in the IDE's Run tool window.
4. **Given** a task has been executed successfully, **When** the developer views the gutter, **Then** the icon updates to a green checkmark (✓) indicating the last run passed.
5. **Given** a task execution failed (non-zero exit code), **When** the developer views the gutter, **Then** the icon updates to a red cross (✗) indicating the last run failed.
6. **Given** the developer right-clicks on a `tasks.register()` line in the editor, **When** they open the context menu, **Then** "Run 'Nadle: build'" and "Debug 'Nadle: build'" options appear in the context menu (not just the gutter).
7. **Given** a run configuration for "Nadle: build" already exists, **When** the developer clicks the play button for `"build"`, **Then** the existing configuration is reused instead of creating a duplicate.
8. **Given** the developer opens the Run/Debug Configurations dialog, **When** they create a new Nadle Task configuration, **Then** they can manually enter a task name and execute it.
9. **Given** a nadle config file with multiple task registrations, **When** viewing the file, **Then** each `tasks.register()` line has its own independent play button with the correct task name.

---

### Edge Cases

- What happens when the Nadle language server binary is not installed or not found on the system? The plugin should degrade gracefully and continue providing existing run icon functionality.
- How does the plugin behave when the language server process crashes or becomes unresponsive? The plugin should attempt to restart the server and log the error without interrupting the developer.
- What happens when a developer opens a non-nadle TypeScript file? The LSP should not activate or provide diagnostics for files that don't match the `nadle.config.*` naming pattern.
- How does the plugin behave when multiple `nadle.config.ts` files exist in a monorepo project? Each file should be analyzed independently by the language server.
- What happens when the user opens a very large nadle config file (e.g., 500+ task registrations)? The language server should handle it within reasonable time without freezing the editor.
- What happens when Node.js is not installed on the system? The plugin should detect this and notify the user that Node.js is required for language intelligence features.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Plugin MUST locate and launch the Nadle language server by searching the project's `node_modules` first, then falling back to a globally installed `@nadle/language-server`. No manual path configuration is required.
- **FR-002**: Plugin MUST display real-time diagnostics (errors and warnings) from the language server in the editor, including invalid task names, duplicate registrations, and unresolved dependencies.
- **FR-003**: Plugin MUST provide autocompletion for task names within `dependsOn` string literals, triggered by typing inside quotes.
- **FR-004**: Plugin MUST display hover information (task metadata: name, form, description, dependencies, group, inputs, outputs) when hovering over task name strings.
- **FR-005**: Plugin MUST support go-to-definition navigation from `dependsOn` references to the corresponding `tasks.register()` call.
- **FR-006**: Plugin MUST activate language intelligence only for files matching the nadle config pattern (`nadle.config.ts`, `nadle.config.js`, `nadle.config.mts`, `nadle.config.mjs`, `nadle.config.cts`, `nadle.config.cjs`).
- **FR-007**: Plugin MUST display a green play button (▶) in the gutter next to every `tasks.register()` line in nadle config files.
- **FR-008**: Plugin MUST show a context menu with "Run" and "Debug" options when the developer clicks the gutter play button. "Debug" launches the task's Node.js process with the `--inspect` flag and attaches IntelliJ's built-in debugger, enabling breakpoints in task handler code.
- **FR-009**: Plugin MUST provide "Run" and "Debug" options in the editor right-click context menu when the cursor is on a `tasks.register()` line. Debug behavior is identical to FR-008.
- **FR-010**: Plugin MUST update the gutter icon to a green checkmark (✓) after a successful task execution and a red cross (✗) after a failed execution (non-zero exit code). Pass/fail icons MUST reset to the default play button (▶) when the file is edited, since code changes invalidate previous results.
- **FR-011**: Plugin MUST reuse existing run configurations for a task name instead of creating duplicates when the play button is clicked multiple times.
- **FR-012**: Plugin MUST execute tasks via `npx nadle <taskName>` in the IDE's Run tool window with full console output.
- **FR-013**: Plugin MUST support manual creation and editing of Nadle Task run configurations via the Run/Debug Configurations dialog.
- **FR-014**: Plugin MUST handle language server unavailability gracefully — if the server cannot be started, the plugin MUST still provide gutter play buttons and task execution without language intelligence features, and no error dialogs should be shown to the user.
- **FR-015**: Plugin MUST support incremental document synchronization so that edits are reflected in diagnostics without requiring a file save.

### Key Entities

- **Nadle Config File**: A project configuration file following the naming pattern `nadle.config.{ts,js,mts,mjs,cts,cjs}` that contains task definitions via `tasks.register()`.
- **Task Registration**: A call to `tasks.register("name", ...)` that defines a runnable task with optional configuration (description, group, dependencies, inputs, outputs).
- **Language Server**: The external Nadle LSP process (`@nadle/language-server`) that analyzes nadle config files and provides diagnostics, completions, hover, and definition capabilities via the Language Server Protocol over stdio.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Developers see diagnostic markers within 1 second of opening or modifying a nadle config file.
- **SC-002**: Autocompletion popup appears within 500ms of being triggered inside a `dependsOn` string literal.
- **SC-003**: Hover information displays within 500ms of hovering over a task name.
- **SC-004**: Go-to-definition navigates to the correct location within 500ms of Ctrl+Click.
- **SC-005**: Every `tasks.register()` line displays a gutter play button and the correct task name appears in the context menu within 1 second of file open.
- **SC-006**: Gutter icon updates to pass/fail status within 2 seconds of task execution completing.
- **SC-007**: Plugin degrades gracefully when the language server is unavailable — zero crashes or error dialogs shown to the user; gutter play buttons and task execution remain operational.

## Assumptions

- **A-001**: The Nadle language server (`@nadle/language-server`) is installed either as a project dependency (in `node_modules`) or globally. The plugin resolves the server binary automatically — project-local first, then global fallback.
- **A-002**: Node.js is installed on the developer's machine (required for running the language server process).
- **A-003**: The language server uses stdio transport for communication, consistent with the existing VS Code extension's integration pattern.
- **A-004**: The target IntelliJ platform version (2024.2+) provides sufficient LSP client capabilities for the features described.
- **A-005**: The existing run icon and task execution functionality (regex-based gutter icons, `npx nadle` execution) will coexist with LSP-provided features without conflict.
