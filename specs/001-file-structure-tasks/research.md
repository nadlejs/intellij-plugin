# Research: File Structure Popup Shows Nadle Tasks

## R-001: Structure View Extension Point for Existing File Types

**Decision**: Use `lang.psiStructureViewFactory` registered for both `JavaScript` and `TypeScript` languages.

**Rationale**: Nadle config files are JS/TS files that already have a default structure view from the JavaScript plugin. `PsiStructureViewFactory` lets us intercept and provide a custom view only for files matching `nadle.config.[cm]?[jt]s`, returning `null` for all other JS/TS files so they keep their default behavior.

**Alternatives considered**:
- `lang.structureViewExtension` — extends the existing JS structure view rather than replacing it; would show JS classes/functions alongside tasks, which is noisy for config files
- Custom file type — too invasive; would lose JS/TS language support (syntax highlighting, LSP, etc.)

## R-002: Implementation Architecture

**Decision**: Three new classes following IntelliJ's layered Structure View pattern:

1. **NadleStructureViewFactory** (`PsiStructureViewFactory`)
   - Guards on `NadleFileUtil.isNadleConfigFile()`
   - Returns `TreeBasedStructureViewBuilder` for nadle files, `null` otherwise

2. **NadleStructureViewModel** (`TextEditorBasedStructureViewModel`)
   - Root element wraps the `PsiFile`
   - `getRoot()` returns a file-level element whose children are task elements

3. **NadleTaskStructureViewElement** (`StructureViewTreeElement`)
   - Wraps the PSI element at the `tasks.register()` call site
   - `getPresentation()` returns task name + `NadleIcons.Nadle`
   - `navigate()` delegates to `PsiElement.navigate()`
   - `getChildren()` returns empty array (tasks are leaf nodes)

**Rationale**: This is the standard IntelliJ pattern used by Properties, JSON, and other plugins. Minimal code, maximum platform integration (filtering, sorting, autoscroll all work automatically).

## R-003: Task Discovery from PSI

**Decision**: Walk the PsiFile's children using `PsiTreeUtil` or manual traversal, matching text against `NadleFileUtil.TASK_REGISTER_PATTERN`. Collect the PSI elements at the match offsets.

**Rationale**: Reuses the existing regex pattern that's already proven in `NadleTaskRunLineMarkerContributor` and `NadleTaskConfigurationProducer`. No need for full AST parsing — regex on text content is sufficient and language-agnostic (works for both JS and TS).

**Approach**: Use `file.text` to find all matches with offsets via `TASK_REGISTER_PATTERN.findAll()`, then `file.findElementAt(matchOffset)` to get the navigable PSI element.

## R-004: Filtering / Search Behavior

**Decision**: No custom implementation needed. IntelliJ's File Structure popup provides built-in type-to-filter (including CamelHump matching) based on `ItemPresentation.getPresentableText()`.

**Rationale**: The platform handles this automatically as long as the tree elements return proper `ItemPresentation`. This satisfies FR-005 with zero code.

## R-005: plugin.xml Registration

**Decision**: Register `lang.psiStructureViewFactory` for both `JavaScript` and `TypeScript` languages.

**Rationale**: Nadle config files can be either `.js`/`.cjs`/`.mjs` (JavaScript) or `.ts`/`.cts`/`.mts` (TypeScript). Both need the custom structure view. The factory's `isNadleConfigFile()` guard ensures only nadle files are affected.
