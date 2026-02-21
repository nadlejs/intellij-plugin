# Implementation Plan: IntelliJ LSP Integration for Nadle Language Server

**Branch**: `001-intellij-lsp-integration` | **Date**: 2026-02-21 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/001-intellij-lsp-integration/spec.md`

## Summary

Integrate the existing Nadle language server (`@nadle/language-server`) into the IntelliJ plugin to provide diagnostics, completions, hover, and go-to-definition for `nadle.config.*` files. Replace the current `LineMarkerProvider` with `RunLineMarkerContributor` to deliver vitest/mocha-style gutter play buttons with Run/Debug context menus and pass/fail state tracking. Use IntelliJ's built-in LSP API (`com.intellij.platform.lsp`) as an optional dependency so the plugin degrades gracefully on IDEs without LSP support.

## Technical Context

**Language/Version**: Kotlin 2.1.20, JVM 21
**Primary Dependencies**: IntelliJ Platform SDK 2024.2.5, IntelliJ Platform Gradle Plugin 2.5.0, JavaScript plugin
**Storage**: N/A (state managed by IntelliJ's `TestStateStorage`)
**Testing**: JUnit 4 (existing), IntelliJ test framework for plugin testing
**Target Platform**: IntelliJ IDEA Ultimate 2024.2+ / WebStorm 2024.2+ (LSP features); IntelliJ Community 2024.2+ (gutter icons only)
**Project Type**: Single IntelliJ plugin project
**Performance Goals**: Diagnostics <1s, completions/hover/definition <500ms (delegated to language server)
**Constraints**: Plugin must work without LSP module (graceful degradation); Node.js 22+ required for language server
**Scale/Scope**: ~10 Kotlin source files, ~800-1000 lines total

## Constitution Check

*No constitution file found. Proceeding without constitution gates.*

## Project Structure

### Documentation (this feature)

```text
specs/001-intellij-lsp-integration/
├── plan.md                                # This file
├── spec.md                                # Feature specification
├── research.md                            # Phase 0: technology decisions
├── data-model.md                          # Phase 1: entity model
├── quickstart.md                          # Phase 1: developer setup guide
├── checklists/
│   └── requirements.md                    # Spec quality checklist
├── contracts/
│   ├── plugin-extension-points.md         # plugin.xml registration contract
│   ├── lsp-server-lifecycle.md            # Server discovery, startup, error handling
│   └── run-configuration.md              # Run/debug configuration and gutter state
└── tasks.md                               # Phase 2 output (via /speckit.tasks)
```

### Source Code (repository root)

```text
src/main/
├── kotlin/com/github/nadlejs/intellij/plugin/
│   ├── NadleFileUtil.kt                   # NEW — shared file pattern matching
│   ├── NadleLspServerSupportProvider.kt   # NEW — LSP server lifecycle (optional module)
│   ├── NadleLspServerDescriptor.kt        # NEW — LSP server startup & file matching
│   ├── NadleTaskRunLineMarkerContributor.kt # NEW — replaces LineMarkerProvider
│   ├── NadleTaskExecutionListener.kt      # NEW — tracks pass/fail for gutter state
│   ├── NadleTaskConfigurationType.kt      # MODIFY — use consistent icon, register properly
│   ├── NadleTaskRunConfiguration.kt       # MODIFY — add debug mode support
│   ├── NadleTaskConfigurationProducer.kt  # MODIFY — support all config file extensions
│   ├── NadleTaskSettingsEditor.kt         # KEEP — minimal changes
│   └── NadleTaskRunConfigurationOptions.kt # KEEP or REMOVE — currently unused
├── resources/META-INF/
│   ├── plugin.xml                         # MODIFY — new extension point registrations
│   └── nadle-lsp.xml                      # NEW — optional LSP extensions
└── resources/messages/
    └── MyBundle.properties                # MODIFY — add new string resources

src/test/
└── kotlin/com/github/nadlejs/intellij/plugin/
    ├── NadleFileUtilTest.kt               # NEW — file pattern matching tests
    ├── NadleRunLineMarkerTest.kt          # NEW — gutter icon integration test
    └── NadleLspServerDescriptorTest.kt    # NEW — server discovery tests
```

**Structure Decision**: Single IntelliJ plugin project following the existing Gradle-based structure. New files are added alongside existing ones in the same package. The LSP support is split into a separate `nadle-lsp.xml` config file for optional loading.

## Implementation Phases

### Phase 1: Foundation — File Utilities and Build Configuration

**Goal**: Set up the shared infrastructure that all other phases depend on.

**Changes**:
1. **`gradle.properties`**: Change `platformType = IC` to `platformType = IU` to access LSP API during development. Plugin still works on IC at runtime via optional dependency.
2. **`NadleFileUtil.kt`** (new): Extract the nadle config file pattern matching into a shared utility.
   - `isNadleConfigFile(file: VirtualFile): Boolean` — matches `nadle.config.{ts,js,mts,mjs,cts,cjs}`
   - `TASK_REGISTER_PATTERN: Regex` — shared regex for `tasks.register("name", ...)`
   - `extractTaskName(text: String): String?` — extracts task name from matched text
3. **`plugin.xml`**: Add missing `configurationType` and `runConfigurationProducer` registrations. Remove the old `lineMarkerProvider` entry.

**Depends on**: Nothing
**Validates**: Build compiles, existing tests pass

### Phase 2: Enhanced Gutter Play Buttons

**Goal**: Replace `LineMarkerProvider` with `RunLineMarkerContributor` for proper Run/Debug context menus.

**Changes**:
1. **`NadleTaskRunLineMarkerContributor.kt`** (new): Extends `RunLineMarkerContributor`.
   - `getInfo(element: PsiElement): Info?` — returns `withExecutorActions()` for leaf PSI elements matching `tasks.register()` in nadle config files.
   - Uses `NadleFileUtil` for file and pattern matching.
   - Returns appropriate icon from `getTestStateIcon()` based on `TestStateStorage` state.
   - Registers for both TypeScript and JavaScript languages in `plugin.xml`.
2. **`NadleTaskLineMarkerProvider.kt`**: DELETE this file (replaced by RunLineMarkerContributor).
3. **`NadleTaskConfigurationProducer.kt`** (modify): Update to use `NadleFileUtil.isNadleConfigFile()` instead of hardcoded `nadle.config.ts` check. Support all config file extensions.
4. **`plugin.xml`**: Replace `lineMarkerProvider` with two `runLineMarkerContributor` entries (TypeScript + JavaScript).

**Depends on**: Phase 1
**Validates**: Gutter play buttons appear, Run/Debug context menu works, clicking Run executes the task

### Phase 3: Pass/Fail Gutter State Tracking

**Goal**: Update gutter icons to show pass (✓) or fail (✗) after task execution.

**Changes**:
1. **`NadleTaskExecutionListener.kt`** (new): Implements `ExecutionListener`.
   - Subscribes to `ExecutionManager.EXECUTION_TOPIC`.
   - On `processTerminated`: checks if the configuration is `NadleTaskRunConfiguration`, reads exit code, writes result to `TestStateStorage` using URL scheme `nadle:task://<filePath>#<taskName>`.
   - On document change (via `DocumentListener` or `BulkFileListener`): clears `TestStateStorage` entries for the edited file, forcing gutter icons back to ▶.
   - Registered as a project-level service or via `postStartupActivity`.
2. **`NadleTaskRunLineMarkerContributor.kt`** (modify): Use `getTestStateIcon("nadle:task://...", project, false)` to read execution state when determining the icon.

**Depends on**: Phase 2
**Validates**: After running a task, gutter icon changes to ✓ or ✗. After editing the file, icons reset to ▶.

### Phase 4: Debug Support

**Goal**: Add debug execution mode that launches tasks with Node.js `--inspect-brk`.

**Changes**:
1. **`NadleTaskRunConfiguration.kt`** (modify):
   - Override `getState(executor, environment)` to detect the debug executor.
   - When debug executor is used: build command line with `node --inspect-brk <nadle-binary> <taskName>` instead of `npx nadle <taskName>`.
   - Resolve the nadle binary path from `node_modules/.bin/nadle` or global.
   - Create a `V8DebugRunProfileState` or equivalent that IntelliJ's Node.js debugger can attach to.

**Depends on**: Phase 2
**Validates**: Selecting "Debug" from gutter menu launches task with debugger attached; breakpoints in task handlers work.

### Phase 5: LSP Integration

**Goal**: Connect to the Nadle language server for diagnostics, completions, hover, and go-to-definition.

**Changes**:
1. **`NadleLspServerDescriptor.kt`** (new): Extends `ProjectWideLspServerDescriptor`.
   - `isSupportedFile(file)`: delegates to `NadleFileUtil.isNadleConfigFile()`.
   - `createCommandLine()`: resolves the language server binary path (project `node_modules` first, then global) and returns `GeneralCommandLine("node", resolvedPath)`.
   - Server discovery: checks `<projectRoot>/node_modules/@nadle/language-server/server.mjs`, then falls back to global resolution.
2. **`NadleLspServerSupportProvider.kt`** (new): Implements `LspServerSupportProvider`.
   - `fileOpened(project, file, serverStarter)`: checks `NadleFileUtil.isNadleConfigFile(file)` and calls `serverStarter.ensureServerStarted(NadleLspServerDescriptor(project))`.
   - Handles server unavailability gracefully (logs warning, does not throw).
3. **`nadle-lsp.xml`** (new): Registers `platform.lsp.serverSupportProvider`.
4. **`plugin.xml`** (modify): Add `<depends optional="true" config-file="nadle-lsp.xml">com.intellij.modules.lsp</depends>`.

**Depends on**: Phase 1
**Validates**: Opening a nadle config file with errors shows diagnostic markers. Autocomplete works in `dependsOn`. Hover shows task metadata. Ctrl+Click navigates to definition.

### Phase 6: Graceful Degradation and Polish

**Goal**: Ensure the plugin works correctly when LSP is unavailable and handles all edge cases.

**Changes**:
1. **`NadleLspServerDescriptor.kt`** (modify): Add robust error handling for missing Node.js, missing server binary, and server crashes. Log warnings via `Logger`.
2. **`NadleTaskRunConfiguration.kt`** (modify): Validate that `npx` or `node` is available before execution. Show a notification if Node.js is not found.
3. **Integration tests**: Add tests for:
   - File pattern matching across all extensions
   - Gutter icon presence on `tasks.register()` lines
   - Server discovery logic (mock file system)
   - Graceful behavior when LSP module is not loaded

**Depends on**: Phases 3, 4, 5
**Validates**: Plugin loads without errors on Community Edition (no LSP). No crashes when Node.js is missing. All edge cases from spec are handled.

## Risk Assessment

| Risk | Likelihood | Impact | Mitigation |
| ---- | ---------- | ------ | ---------- |
| `RunLineMarkerContributor` doesn't work well with TypeScript PSI | Medium | High | Fall back to `LineMarkerProvider` if needed; test early in Phase 2 |
| Debug mode (`--inspect-brk`) doesn't integrate cleanly with IntelliJ's Node.js debugger | Medium | Medium | Phase 4 is independent; can ship without debug initially |
| Built-in LSP API limitations for specific nadle server features | Low | Medium | The server's capabilities are simple (no custom extensions); built-in API covers all 4 features |
| `TestStateStorage` URL scheme conflicts with other plugins | Low | Low | Use unique `nadle:task://` prefix |

## Complexity Tracking

No constitution violations to justify.
