# Tasks: File Structure Popup Shows Nadle Tasks

**Input**: Design documents from `/specs/001-file-structure-tasks/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, quickstart.md

**Tests**: Not requested in spec. No test tasks generated.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2)
- Include exact file paths in descriptions

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Register extension points in plugin descriptor

- [x] T001 Register `lang.psiStructureViewFactory` for JavaScript and TypeScript languages in `src/main/resources/META-INF/plugin.xml`

---

## Phase 2: User Story 1 - View All Tasks via File Structure (Priority: P1) MVP

**Goal**: Pressing Cmd+F12 in a nadle config file lists all `tasks.register()` calls with the Nadle icon. Selecting a task navigates to its definition. Non-nadle JS/TS files keep their default structure view.

**Independent Test**: Open a nadle config file, press Cmd+F12, verify all tasks appear with Nadle icons. Click a task, verify editor navigates to the correct line. Open a regular `.js` file, press Cmd+F12, verify default JavaScript structure view appears.

### Implementation for User Story 1

- [x] T002 [P] [US1] Create `NadleTaskStructureViewElement` implementing `StructureViewTreeElement` in `src/main/kotlin/com/github/nadlejs/intellij/plugin/NadleTaskStructureViewElement.kt` — wraps a PSI element at a `tasks.register()` call site; `getPresentation()` returns task name + `NadleIcons.Nadle`; `navigate()` delegates to `PsiElement.navigate()`; `getChildren()` returns empty array (leaf nodes)
- [x] T003 [P] [US1] Create `NadleStructureViewModel` extending `TextEditorBasedStructureViewModel` in `src/main/kotlin/com/github/nadlejs/intellij/plugin/NadleStructureViewModel.kt` — root element scans `file.text` with `NadleFileUtil.TASK_REGISTER_PATTERN.findAll()` to find match offsets, resolves each to a PSI element via `file.findElementAt()`, wraps in `NadleTaskStructureViewElement` children
- [x] T004 [US1] Create `NadleStructureViewFactory` implementing `PsiStructureViewFactory` in `src/main/kotlin/com/github/nadlejs/intellij/plugin/NadleStructureViewFactory.kt` — guards on `NadleFileUtil.isNadleConfigFile()`; returns `TreeBasedStructureViewBuilder` for nadle files, `null` otherwise (depends on T002, T003)
- [x] T005 [US1] Verify compilation with `./gradlew compileKotlin` and manual test with `./gradlew runIde`

**Checkpoint**: User Story 1 is fully functional — File Structure shows tasks in nadle config files, navigation works, non-nadle files are unaffected.

---

## Phase 3: User Story 2 - Filter Tasks by Typing (Priority: P2)

**Goal**: Type-to-filter narrows the task list in the File Structure popup.

**Independent Test**: Open File Structure in a config file with multiple tasks, type a partial name, verify the list filters correctly.

**Note**: This story requires zero code. IntelliJ's File Structure popup provides built-in type-to-filter based on `ItemPresentation.getPresentableText()`, which is already implemented in T002. This phase is verification-only.

- [x] T006 [US2] Verify type-to-filter works in `./gradlew runIde` — open a nadle config with multiple tasks, type partial name, confirm filtering works including CamelHump matching

**Checkpoint**: Both user stories are complete and independently verified.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — start immediately
- **User Story 1 (Phase 2)**: Depends on T001 (plugin.xml registration)
- **User Story 2 (Phase 3)**: Depends on US1 completion (verification only)

### Within User Story 1

- T002 and T003 can run in parallel (different files, no dependencies)
- T004 depends on T002 and T003 (factory references both classes)
- T005 depends on T004 (compile check)

### Parallel Opportunities

```text
# After T001, launch T002 and T003 in parallel:
Task: "Create NadleTaskStructureViewElement in .../NadleTaskStructureViewElement.kt"
Task: "Create NadleStructureViewModel in .../NadleStructureViewModel.kt"

# Then sequential:
Task: T004 → T005 → T006
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Register extension points (T001)
2. Complete Phase 2: Implement all 3 classes (T002-T004), verify (T005)
3. **STOP and VALIDATE**: Cmd+F12 shows tasks, navigation works, non-nadle files unaffected
4. Deploy/demo if ready

### Full Delivery

1. Complete MVP above
2. Verify US2 filtering (T006) — should work with zero code changes

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- US2 (filtering) is a "free" feature from the IntelliJ platform — no implementation needed
- Total: 6 tasks (1 setup, 4 US1, 1 US2 verification)
- Parallel opportunities: T002 + T003 can run simultaneously
