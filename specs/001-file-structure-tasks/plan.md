# Implementation Plan: File Structure Popup Shows Nadle Tasks

**Branch**: `001-file-structure-tasks` | **Date**: 2026-02-21 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-file-structure-tasks/spec.md`

## Summary

Add a custom Structure View provider for nadle config files so that invoking File Structure (Cmd+F12) lists all `tasks.register()` calls with the Nadle icon. Selecting an entry navigates to the task definition. Uses IntelliJ's `PsiStructureViewFactory` extension point, reusing the existing `NadleFileUtil` pattern matching and `NadleIcons`.

## Technical Context

**Language/Version**: Kotlin 2.1.20, JVM 21
**Primary Dependencies**: IntelliJ Platform SDK 2024.2.5, JavaScript plugin
**Storage**: N/A
**Testing**: `./gradlew compileKotlin` + manual `./gradlew runIde` verification
**Target Platform**: IntelliJ IDEA 2024.2 through 2025.3
**Project Type**: Single IntelliJ plugin project
**Performance Goals**: Structure view populates within 1 second
**Constraints**: Must not break default File Structure for non-nadle JS/TS files
**Scale/Scope**: Typically 1-30 tasks per config file

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

No constitution file found. Gate passes by default.

## Project Structure

### Documentation (this feature)

```text
specs/001-file-structure-tasks/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── quickstart.md        # Phase 1 output
└── checklists/
    └── requirements.md  # Spec quality checklist
```

### Source Code (repository root)

```text
src/main/kotlin/com/github/nadlejs/intellij/plugin/
├── NadleFileUtil.kt                    # Existing - task name extraction
├── NadleIcons.kt                       # Existing - Nadle icon
├── NadleStructureViewFactory.kt        # NEW - PsiStructureViewFactory
├── NadleStructureViewModel.kt          # NEW - TextEditorBasedStructureViewModel
└── NadleTaskStructureViewElement.kt    # NEW - StructureViewTreeElement for tasks

src/main/resources/META-INF/
└── plugin.xml                          # MODIFY - register extension points
```

**Structure Decision**: All new files go in the existing flat plugin package, following the same pattern as other plugin components.
