# Research: Task Explorer Panel

## R1: ToolWindow Registration

**Decision**: Use `<toolWindow>` extension point with a `ToolWindowFactory` implementation.

**Rationale**: Standard IntelliJ pattern. The factory creates the tool window content lazily when first opened. Using `factoryClass` with `anchor="right"` and `icon` attributes matches Gradle/Maven conventions.

**Alternatives considered**:
- Programmatic registration via `ToolWindowManager.registerToolWindow()` — rejected because declarative XML registration is the standard approach and aligns with the Platform-First principle.

## R2: Tree Component

**Decision**: Use `com.intellij.ui.treeStructure.Tree` (or `SimpleTree`) backed by `DefaultTreeModel` with custom `DefaultMutableTreeNode` user objects.

**Rationale**: IntelliJ's `Tree` extends `JBTree` which provides built-in filtering, drag support, and platform-consistent look. Custom tree nodes wrapping `NadleTask` and config file path provide the data model.

**Alternatives considered**:
- `AbstractTreeStructure` with `AbstractTreeBuilder` — more powerful but heavyweight for a simple two-level tree. Rejected per Simplicity principle.
- `ListPanel` — doesn't support hierarchical grouping.

## R3: Task Execution from ToolWindow

**Decision**: Extract the run-configuration creation and execution pattern from `NadleTaskSearchEverywhereContributor.processSelectedItem()` into a shared utility method in `NadleTaskRunner` (new utility in `run/` package).

**Rationale**: Both SearchEverywhere and ToolWindow need to create/find a run configuration and execute it. Extracting avoids duplication. The pattern: find existing config → create if missing → set working directory → execute via `ExecutionUtil.runConfiguration()`.

**Alternatives considered**:
- Duplicating the logic in the ToolWindow — rejected to avoid code duplication.
- Using `ProgramRunner` directly — rejected because `ExecutionUtil.runConfiguration()` is the standard high-level API.

## R4: File Change Watching for Auto-Refresh

**Decision**: Subscribe to `VirtualFileManager.VFS_CHANGES` via `BulkFileListener` on the project message bus, matching the existing pattern in `NadleTaskExecutionListener`.

**Rationale**: The plugin already uses this pattern for clearing task state on file changes. The ToolWindow can subscribe to the same topic and trigger a tree refresh when nadle config files are created, modified, or deleted.

**Alternatives considered**:
- `FileDocumentManagerListener` — only fires on save, misses external changes.
- Polling with timer — wasteful and not reactive. Rejected.

## R5: Background Scanning

**Decision**: Use `ApplicationManager.getApplication().executeOnPooledThread()` for the initial scan and refreshes, then update the tree model on the EDT via `SwingUtilities.invokeLater()` or `ApplicationManager.getApplication().invokeLater()`.

**Rationale**: `NadleTaskScanner.scanTasksDetailed()` walks the filesystem which can be slow on large projects. Must not block the UI thread (FR-013). The pooled thread → EDT pattern is standard IntelliJ practice.

**Alternatives considered**:
- Coroutines — IntelliJ's coroutine support is available but adds complexity for a simple scan→update flow. Rejected per Simplicity principle.

## R6: ToolWindow Placement and Visibility

**Decision**: Register with `anchor="right"` and `conditionClass` or lazy initialization that checks for nadle config file presence. The tool window should be available in all projects but only auto-activate when nadle files are detected.

**Rationale**: Matches Gradle/Maven conventions (right sidebar). Lazy activation avoids clutter in non-nadle projects.

## R7: Debug Action

**Decision**: Reuse the existing debug execution path. `NadleTaskRunConfiguration` already supports debug mode (uses `node --inspect-brk` instead of `npx`). Use `DefaultDebugExecutor.getDebugExecutorInstance()` instead of `DefaultRunExecutor.getRunExecutorInstance()`.

**Rationale**: All debug infrastructure already exists. The only difference is passing the debug executor.

## R8: Package Structure

**Decision**: Create a new `toolwindow/` subpackage under `com.github.nadlejs.intellij.plugin`.

**Rationale**: Aligns with Constitution Principle VI (Package Organization) — new subpackages are appropriate when a clear domain boundary emerges with 3+ related classes. The ToolWindow will have at least: Factory, Panel (tree builder + actions), and potentially tree node classes.
