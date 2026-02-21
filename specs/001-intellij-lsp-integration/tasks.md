# Tasks: IntelliJ LSP Integration for Nadle Language Server

**Input**: Design documents from `/specs/001-intellij-lsp-integration/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Not explicitly requested. Test tasks omitted.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US5)
- Include exact file paths in descriptions

## Path Conventions

- **Single project**: `src/main/kotlin/com/github/nadlejs/intellij/plugin/` for source
- Resources: `src/main/resources/META-INF/` for plugin config

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Build configuration changes and project initialization

- [x] T001 Update `platformType = IC` to `platformType = IU` in `gradle.properties` to enable LSP API access during development
- [x] T002 Verify project builds successfully with `./gradlew buildPlugin` after platform type change

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared utility and plugin registration fixes that ALL user stories depend on

**CRITICAL**: No user story work can begin until this phase is complete

- [x] T003 Create `NadleFileUtil.kt` in `src/main/kotlin/com/github/nadlejs/intellij/plugin/NadleFileUtil.kt` with: `isNadleConfigFile(file: VirtualFile): Boolean` matching `nadle.config.{ts,js,mts,mjs,cts,cjs}`, shared `TASK_REGISTER_PATTERN` regex, and `extractTaskName(text: String): String?` helper
- [x] T004 Update `src/main/resources/META-INF/plugin.xml`: remove `lineMarkerProvider` entry, add `configurationType` registration for `NadleTaskConfigurationType`, add `runConfigurationProducer` registration for `NadleTaskConfigurationProducer`, add optional LSP dependency `<depends optional="true" config-file="nadle-lsp.xml">com.intellij.modules.lsp</depends>`
- [x] T005 Update `NadleTaskConfigurationType.kt` in `src/main/kotlin/com/github/nadlejs/intellij/plugin/NadleTaskConfigurationType.kt`: change icon to `AllIcons.RunConfigurations.TestState.Run` for consistency with gutter icons
- [x] T006 Update `NadleTaskConfigurationProducer.kt` in `src/main/kotlin/com/github/nadlejs/intellij/plugin/NadleTaskConfigurationProducer.kt`: replace hardcoded `nadle.config.ts` check with `NadleFileUtil.isNadleConfigFile()`, replace inline regex with `NadleFileUtil.TASK_REGISTER_PATTERN` and `NadleFileUtil.extractTaskName()`

**Checkpoint**: Build compiles. ConfigurationType and ConfigurationProducer are properly registered. File pattern matching supports all nadle config extensions.

---

## Phase 3: User Story 5 - Enhanced Task Runner Gutter Experience (Priority: P1) MVP

**Goal**: Replace `LineMarkerProvider` with `RunLineMarkerContributor` to deliver vitest/mocha-style gutter play buttons with Run/Debug context menu, pass/fail state, and debug support.

**Independent Test**: Open a `nadle.config.ts` file with multiple `tasks.register()` calls. Verify each line has a play button, left-clicking shows Run/Debug context menu, selecting Run executes the task, and after execution the icon updates to pass/fail. Editing the file resets icons to play button.

### Implementation for User Story 5

- [x] T007 [US5] Create `NadleTaskRunLineMarkerContributor.kt` in `src/main/kotlin/com/github/nadlejs/intellij/plugin/NadleTaskRunLineMarkerContributor.kt`: extend `RunLineMarkerContributor`, implement `getInfo(element: PsiElement): Info?` that checks `NadleFileUtil.isNadleConfigFile()` on the containing file, matches leaf PSI elements against `NadleFileUtil.TASK_REGISTER_PATTERN`, and returns `withExecutorActions(icon)` where icon is determined by `getTestStateIcon()` using URL scheme `nadle:task://<filePath>#<taskName>`
- [x] T008 [US5] Register `NadleTaskRunLineMarkerContributor` in `src/main/resources/META-INF/plugin.xml`: add two `runLineMarkerContributor` entries — one for `language="TypeScript"` and one for `language="JavaScript"` — both pointing to `NadleTaskRunLineMarkerContributor`
- [x] T009 [US5] Delete `src/main/kotlin/com/github/nadlejs/intellij/plugin/NadleTaskLineMarkerProvider.kt` (replaced by `RunLineMarkerContributor`)
- [x] T010 [US5] Create `NadleTaskExecutionListener.kt` in `src/main/kotlin/com/github/nadlejs/intellij/plugin/NadleTaskExecutionListener.kt`: implement `ExecutionListener`, subscribe to `ExecutionManager.EXECUTION_TOPIC`, on `processTerminated` check if configuration is `NadleTaskRunConfiguration`, write pass/fail result to `TestStateStorage` using URL `nadle:task://<filePath>#<taskName>` with magnitude `PASSED_INDEX` (exit code 0) or `FAILED_INDEX` (non-zero), trigger editor reparse via `DaemonCodeAnalyzer.restart()`
- [x] T011 [US5] Add file edit listener to `NadleTaskExecutionListener.kt`: subscribe to `BulkFileListener` on `VirtualFileManager.VFS_CHANGES`, when a nadle config file is modified clear all `TestStateStorage` entries matching `nadle:task://<filePath>#*` to reset gutter icons to default play button
- [x] T012 [US5] Register `NadleTaskExecutionListener` in `src/main/resources/META-INF/plugin.xml` as a `postStartupActivity` or project-level listener that subscribes on project open
- [x] T013 [US5] Update `NadleTaskRunConfiguration.kt` in `src/main/kotlin/com/github/nadlejs/intellij/plugin/NadleTaskRunConfiguration.kt`: override `getState(executor, environment)` to detect debug executor (`DefaultDebugExecutor`), when debug mode build command line as `node --inspect-brk <resolved-nadle-binary> <taskName>` resolving nadle binary from `node_modules/.bin/nadle`, when run mode keep existing `npx nadle <taskName>` behavior
- [x] T014 [US5] Store `configFilePath` in `NadleTaskRunConfiguration.kt` alongside `taskName` so the execution listener can construct the correct `TestStateStorage` URL; update `NadleTaskConfigurationProducer.kt` to set `configFilePath` from the PSI context
- [ ] T015 [US5] Verify build compiles and run `./gradlew runIde` to manually test: gutter play buttons appear, Run/Debug context menu shows, task executes, pass/fail icons update, icons reset on file edit

**Checkpoint**: User Story 5 is fully functional. Developers get vitest/mocha-style gutter experience with Run/Debug and pass/fail tracking. This is the MVP — independently testable and deliverable without LSP.

---

## Phase 4: User Story 1 - Real-Time Error Feedback (Priority: P1)

**Goal**: Connect to the Nadle language server to provide real-time diagnostics (errors and warnings) in nadle config files. This also enables US2 (completions), US3 (hover), and US4 (go-to-definition) since all four features come from the language server and IntelliJ's built-in LSP client maps them automatically.

**Independent Test**: Open a `nadle.config.ts` file with an invalid task name like `"123bad"`. Verify an error underline appears. Fix the name and verify the error disappears.

### Implementation for User Story 1

- [x] T016 [P] [US1] Create `NadleLspServerDescriptor.kt` in `src/main/kotlin/com/github/nadlejs/intellij/plugin/NadleLspServerDescriptor.kt`: extend `ProjectWideLspServerDescriptor`, implement `isSupportedFile(file)` delegating to `NadleFileUtil.isNadleConfigFile()`, implement `createCommandLine()` that resolves `node_modules/@nadle/language-server/server.mjs` from project root first then falls back to global resolution, return `GeneralCommandLine("node", resolvedPath)`
- [x] T017 [P] [US1] Create `NadleLspServerSupportProvider.kt` in `src/main/kotlin/com/github/nadlejs/intellij/plugin/NadleLspServerSupportProvider.kt`: implement `LspServerSupportProvider`, in `fileOpened(project, file, serverStarter)` check `NadleFileUtil.isNadleConfigFile(file)` and call `serverStarter.ensureServerStarted(NadleLspServerDescriptor(project))`, wrap in try-catch to handle server unavailability gracefully (log warning, do not throw)
- [x] T018 [US1] Create `src/main/resources/META-INF/nadle-lsp.xml` with `platform.lsp.serverSupportProvider` extension registration pointing to `NadleLspServerSupportProvider`
- [x] T019 [US1] Add server discovery logic to `NadleLspServerDescriptor.kt`: implement `resolveServerPath(project: Project): String?` that checks `<projectBasePath>/node_modules/@nadle/language-server/server.mjs` first, then tries `which nadle-language-server` for global fallback, returns null if not found; handle null in `createCommandLine()` by throwing with descriptive message
- [ ] T020 [US1] Verify LSP integration with `./gradlew runIde`: open a test project with `@nadle/language-server` installed, open `nadle.config.ts`, verify diagnostics appear for invalid task names, duplicates, and unresolved dependencies

**Checkpoint**: US1 (diagnostics) is functional. US2 (completions), US3 (hover), and US4 (go-to-definition) are also functional since IntelliJ's LSP client automatically maps the server's capabilities to IDE features.

---

## Phase 5: User Story 2 - Task Name Autocompletion (Priority: P2)

**Goal**: Verify that autocompletion for task names in `dependsOn` works via the LSP integration from Phase 4. No additional implementation needed — IntelliJ's LSP client handles completions automatically.

**Independent Test**: Place cursor inside `dependsOn: ["` and trigger autocomplete. Verify task names appear.

### Implementation for User Story 2

- [ ] T021 [US2] Verify completions work in `./gradlew runIde`: open a nadle config with multiple tasks, place cursor inside a `dependsOn: ["` string literal, trigger autocomplete (Ctrl+Space), verify task names appear with form and description detail, verify current task is excluded from suggestions

**Checkpoint**: US2 confirmed functional via LSP integration.

---

## Phase 6: User Story 3 - Hover Information (Priority: P3)

**Goal**: Verify that hover information for task names works via the LSP integration. No additional implementation needed.

**Independent Test**: Hover over a task name string and verify metadata popup appears.

### Implementation for User Story 3

- [ ] T022 [US3] Verify hover works in `./gradlew runIde`: hover over a task name in `tasks.register("build", ...)`, verify popup shows task form, description, group, dependencies; hover over a name in `dependsOn` and verify same metadata appears; verify tasks without description/group show minimal popup

**Checkpoint**: US3 confirmed functional via LSP integration.

---

## Phase 7: User Story 4 - Navigate to Definition (Priority: P4)

**Goal**: Verify that go-to-definition from `dependsOn` references works via the LSP integration. No additional implementation needed.

**Independent Test**: Ctrl+Click on a task name in `dependsOn` and verify navigation to `tasks.register()` call.

### Implementation for User Story 4

- [ ] T023 [US4] Verify go-to-definition works in `./gradlew runIde`: Ctrl+Click on a task name inside `dependsOn`, verify cursor navigates to the `tasks.register()` call; verify workspace-qualified names (containing `:`) do not navigate; verify non-existent task names do not navigate

**Checkpoint**: US4 confirmed functional via LSP integration.

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Error handling, edge cases, and graceful degradation across all user stories

- [x] T024 Add Node.js detection to `NadleLspServerDescriptor.kt`: before creating command line, verify `node` is available in PATH; if not found, log a warning with message "Node.js is required for Nadle language intelligence features" and skip LSP initialization
- [x] T025 Add graceful degradation handling to `NadleLspServerSupportProvider.kt`: ensure no error dialogs are shown to the user when server is unavailable; verify gutter play buttons and task execution continue working without LSP (FR-014)
- [x] T026 Update `NadleTaskRunConfiguration.kt`: add validation in `checkConfiguration()` that `npx` or `node` is available before execution; show user-friendly notification if Node.js is not found
- [x] T027 [P] Remove unused `NadleTaskRunConfigurationOptions.kt` from `src/main/kotlin/com/github/nadlejs/intellij/plugin/NadleTaskRunConfigurationOptions.kt` if confirmed unused after all changes
- [x] T028 [P] Update `src/main/resources/messages/MyBundle.properties` with new string resources for error messages, notifications, and display names
- [x] T029 Run full build and verification: `./gradlew buildPlugin && ./gradlew check` — ensure zero compilation errors, all tests pass, plugin verifier passes
- [ ] T030 Final manual validation following `quickstart.md`: test all features end-to-end in a real nadle project per the verification steps

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **Foundational (Phase 2)**: Depends on Phase 1 — BLOCKS all user stories
- **US5 (Phase 3)**: Depends on Phase 2 — can start immediately after foundational
- **US1 (Phase 4)**: Depends on Phase 2 — can run in parallel with Phase 3
- **US2 (Phase 5)**: Depends on Phase 4 (LSP must be working)
- **US3 (Phase 6)**: Depends on Phase 4 (LSP must be working)
- **US4 (Phase 7)**: Depends on Phase 4 (LSP must be working)
- **Polish (Phase 8)**: Depends on Phases 3, 4

### User Story Dependencies

- **US5 (P1)**: Independent — gutter icons work without LSP
- **US1 (P1)**: Independent of US5 — requires LSP setup only
- **US2 (P2)**: Depends on US1 (LSP infrastructure)
- **US3 (P3)**: Depends on US1 (LSP infrastructure)
- **US4 (P4)**: Depends on US1 (LSP infrastructure)

### Within Each User Story

- Shared utilities (NadleFileUtil) before story-specific code
- Plugin.xml registrations before implementation classes
- Core functionality before state tracking
- State tracking before debug support

### Parallel Opportunities

- T016 and T017 can run in parallel (different files, no dependencies)
- Phase 3 (US5) and Phase 4 (US1) can run in parallel after Phase 2
- Phases 5, 6, 7 (US2-US4) are verification-only and can run in parallel after Phase 4
- T027 and T028 can run in parallel (different files)

---

## Parallel Example: Phase 3 + Phase 4

```bash
# After Phase 2 completes, these two streams can run in parallel:

# Stream A: Gutter play buttons (US5)
T007 → T008 → T009 → T010 → T011 → T012 → T013 → T014 → T015

# Stream B: LSP integration (US1)
T016 + T017 (parallel) → T018 → T019 → T020
```

---

## Implementation Strategy

### MVP First (User Story 5 Only)

1. Complete Phase 1: Setup (T001-T002)
2. Complete Phase 2: Foundational (T003-T006)
3. Complete Phase 3: User Story 5 (T007-T015)
4. **STOP and VALIDATE**: Gutter play buttons work with Run/Debug/pass/fail
5. Deploy/demo — plugin is usable without LSP

### Incremental Delivery

1. Setup + Foundational → Foundation ready
2. Add US5 → Test independently → **MVP!** (gutter play buttons)
3. Add US1 → Test independently → Diagnostics working
4. Verify US2/US3/US4 → All LSP features confirmed
5. Polish → Production-ready plugin
6. Each increment adds value without breaking previous features

### Single Developer Strategy

1. Phase 1 + 2 first (foundation)
2. Phase 3 (US5 — gutter buttons, most code)
3. Phase 4 (US1 — LSP, second most code)
4. Phases 5-7 (US2-4 — verification only)
5. Phase 8 (polish)

---

## Notes

- US2, US3, US4 have no implementation tasks — they are delivered automatically by the LSP server via IntelliJ's built-in LSP client mapping. Their phases contain only verification tasks.
- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
