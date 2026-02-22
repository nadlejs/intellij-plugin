<!--
  Sync Impact Report
  ==================
  Version change: N/A → 1.0.0 (initial ratification)
  Modified principles: N/A (first version)
  Added sections:
    - Core Principles (8 principles)
    - Tech Stack Constraints
    - Development Workflow
    - Governance
  Removed sections: N/A
  Templates requiring updates:
    - plan-template.md: ✅ Compatible (Constitution Check section exists)
    - spec-template.md: ✅ Compatible (no constitution-specific references)
    - tasks-template.md: ✅ Compatible (no constitution-specific references)
  Follow-up TODOs: None
-->

# Nadle IntelliJ Plugin Constitution

## Core Principles

### I. Platform-First

Every feature MUST leverage IntelliJ Platform APIs and extension points
rather than custom implementations. The platform provides built-in
behavior for filtering, sorting, navigation, and UI — use it.

- MUST use `PsiStructureViewFactory`, `RunLineMarkerContributor`,
  `LazyRunConfigurationProducer`, and other standard extension points
- MUST NOT reimplement functionality the platform already provides
  (e.g., type-to-filter in File Structure, `Sorter.ALPHA_SORTER`)
- SHOULD prefer `TextFieldWithAutoCompletion` over raw `JTextField`
  when suggestions are available

**Rationale**: Platform integration gives free upgrades, consistent UX,
and avoids maintenance burden of custom UI/behavior.

### II. Guard Pattern

Every extension point registration MUST guard on
`NadleFileUtil.isNadleConfigFile()` to ensure the plugin only activates
for nadle config files and never interferes with standard JS/TS editing.

- MUST return `null` / `false` / skip processing for non-nadle files
- MUST NOT modify behavior of regular JavaScript or TypeScript files
- Guard checks MUST happen as early as possible in the call chain

**Rationale**: The plugin registers for JavaScript and TypeScript
languages globally. Without guards, every JS/TS file would be affected.

### III. Compilation Gate

`./gradlew compileKotlin` MUST pass before any commit or pull request.
No exceptions.

- MUST run compilation check after every code change
- MUST NOT commit code that fails compilation
- CI build status MUST be green before merging

**Rationale**: The IntelliJ Platform SDK has complex type hierarchies.
Compilation is the first and cheapest line of defense.

### IV. Regex-Based Task Discovery

Task names MUST be discovered using `NadleFileUtil.TASK_REGISTER_PATTERN`
regex on file text content. No full AST parsing.

- MUST use `findAll()` for multi-match scenarios, `find()` only for
  single-match extraction
- MUST handle multiline formatting (`tasks\s*\.register`)
- MUST reuse the shared pattern from `NadleFileUtil` — no duplicating
  regex patterns across files
- PSI tree walking for task identification MUST stop when a parent
  contains multiple `tasks.register()` calls

**Rationale**: Regex on text is language-agnostic (works for JS, TS,
CJS, MJS, CTS, MTS), fast, and proven reliable. Full AST parsing
would require language-specific handling.

### V. Explicit Imports

All Kotlin imports MUST be explicit. Wildcard imports (`import foo.*`)
are prohibited.

- MUST expand wildcard imports to individual class imports
- MUST remove unused imports

**Rationale**: Explicit imports make dependencies visible at a glance
and prevent accidental name collisions.

### VI. Package Organization

Source code MUST be organized into logical subpackages under
`com.github.nadlejs.intellij.plugin`:

- `run/` — Run configurations, execution, gutter markers, task scanning
- `lsp/` — Language Server Protocol integration
- `structure/` — File Structure popup (Cmd+F12)
- `navigation/` — Go-to-definition, references
- `util/` — Shared utilities (file detection, icons, Node.js resolver)

New classes MUST be placed in the appropriate subpackage. New
subpackages may be created when a clear domain boundary emerges
(3+ related classes).

**Rationale**: Flat package with 19+ files is hard to navigate.
Subpackages group related functionality and make the codebase
approachable for new contributors.

### VII. Manual Verification

Every feature MUST be manually verified with `./gradlew runIde` before
merging. Automated compilation alone is insufficient — runtime behavior
in the IDE must be confirmed.

- MUST verify the primary user journey works end-to-end
- MUST verify non-nadle JS/TS files are unaffected
- MUST verify navigation, gutter icons, and popups function correctly

**Rationale**: IntelliJ plugin behavior depends on runtime extension
point resolution, PSI tree structure, and UI interactions that cannot
be validated by compilation alone.

### VIII. Simplicity

Start with the simplest approach that works. Avoid abstractions,
indirection, and configurability that aren't needed today.

- MUST NOT add abstraction layers for single implementations
- MUST NOT add configuration for behaviors with only one valid option
- SHOULD prefer 3 similar lines over a premature helper function
- MUST NOT add error handling for scenarios that cannot occur

**Rationale**: Plugin code is read far more than written. Every
abstraction is a cognitive tax on the next person reading the code.

## Tech Stack Constraints

- **Language**: Kotlin 2.1.20 targeting JVM 21
- **Platform**: IntelliJ Platform SDK 2024.2.5 (build range 242–253.*)
- **Build**: Gradle 8.13 with IntelliJ Platform Gradle Plugin 2.5.0
- **Plugin dependency**: JavaScript plugin (for JS/TS language support)
- **Runtime dependency**: Node.js (resolved via `NodeJsResolver`)
- **Execution**: `npx nadle <taskName>` for run, direct `node` for debug
- **Supported file extensions**: `nadle.config.[cm]?[jt]s`

Adding new dependencies MUST be justified and approved. The plugin
MUST remain lightweight with minimal external dependencies.

## Development Workflow

- **Branching**: Never commit directly to `main`. Use feature branches
  with descriptive names (e.g., `001-file-structure-tasks`,
  `chore/repo-polish`).
- **Spec-driven features**: New features SHOULD follow the speckit
  workflow: `/speckit.specify` → `/speckit.plan` → `/speckit.tasks` →
  `/speckit.implement`.
- **Commit discipline**: Commits MUST be atomic and focused. Run
  `./gradlew compileKotlin` before every commit.
- **Pull requests**: PRs MUST include a summary of changes and a test
  plan. Squash-merge is the default merge strategy.
- **plugin.xml**: All extension point class references MUST use
  fully-qualified names matching the actual package structure. Update
  plugin.xml whenever classes are moved or renamed.

## Governance

This constitution defines the non-negotiable engineering principles
for the Nadle IntelliJ Plugin. All code changes, reviews, and
architectural decisions MUST comply with these principles.

- **Amendments**: Any principle change MUST be documented with a version
  bump, rationale, and migration plan if existing code is affected.
- **Versioning**: Constitution follows semantic versioning. MAJOR for
  principle removals/redefinitions, MINOR for new principles or material
  expansions, PATCH for clarifications and wording fixes.
- **Compliance review**: PRs SHOULD be checked against the Guard Pattern
  (Principle II) and Compilation Gate (Principle III) at minimum.
- **Runtime guidance**: See `CLAUDE.md` for day-to-day development
  reference (commands, project structure, key patterns).

**Version**: 1.0.0 | **Ratified**: 2026-02-21 | **Last Amended**: 2026-02-21
