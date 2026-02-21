# Quickstart: File Structure Popup Shows Nadle Tasks

## What This Feature Does

When a developer opens a nadle config file (`nadle.config.ts`, `nadle.config.js`, etc.) and presses Cmd+F12 (File Structure), a popup appears listing all registered tasks. Each task shows the Nadle icon. Clicking a task navigates to its definition. Type-to-filter narrows the list.

## Files to Create/Modify

### New Files (3)

1. **`NadleStructureViewFactory.kt`** — Entry point. Implements `PsiStructureViewFactory`. Checks if the file is a nadle config file; if yes, returns a `TreeBasedStructureViewBuilder` that creates the view model. Returns `null` for non-nadle files.

2. **`NadleStructureViewModel.kt`** — Extends `TextEditorBasedStructureViewModel`. The `getRoot()` method returns a root element that scans the file for `tasks.register()` calls and creates child `NadleTaskStructureViewElement` entries.

3. **`NadleTaskStructureViewElement.kt`** — Implements `StructureViewTreeElement`. Each instance wraps a PSI element at a `tasks.register()` call. Provides the task name via `ItemPresentation`, shows the Nadle icon, and navigates to the source on click.

### Modified Files (1)

4. **`plugin.xml`** — Register two new `lang.psiStructureViewFactory` entries (one for JavaScript, one for TypeScript) pointing to `NadleStructureViewFactory`.

## Verification

```bash
./gradlew compileKotlin    # Must pass
./gradlew runIde           # Open nadle.config.ts, press Cmd+F12, verify tasks appear
```
