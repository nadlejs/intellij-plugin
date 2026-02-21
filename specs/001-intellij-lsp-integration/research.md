# Research: IntelliJ LSP Integration for Nadle Language Server

**Date**: 2026-02-21
**Branch**: `001-intellij-lsp-integration`

## Decision 1: LSP Client Approach

**Decision**: Use IntelliJ's built-in `com.intellij.platform.lsp` API with optional dependency so the plugin degrades gracefully on Community Edition.

**Rationale**:
- Built-in API is maintained by JetBrains with best platform integration
- No extra dependencies for users on Ultimate/WebStorm
- Available since IntelliJ 2023.2; our target is 2024.2+
- Nadle plugin targets TypeScript developers who typically use WebStorm or Ultimate
- Making LSP an optional dependency satisfies FR-014 (graceful degradation)

**Alternatives considered**:
- **LSP4IJ (Red Hat)**: Works on Community Edition but requires users to install a separate plugin. Adds friction. Uses Java Futures instead of Kotlin Coroutines.
- **No LSP / custom inspections**: Would require reimplementing all analysis logic already in the language server. Violates DRY and diverges from the VS Code extension.

## Decision 2: Gutter Run Icons

**Decision**: Replace `LineMarkerProvider` with `RunLineMarkerContributor` for gutter play buttons.

**Rationale**:
- `RunLineMarkerContributor` is the idiomatic IntelliJ approach for run/debug gutter icons
- Automatically provides the standard Run/Debug/Profile context menu via `ExecutorAction`
- Integrates implicitly with `LazyRunConfigurationProducer` through PSI context
- Same pattern used by JUnit, Vitest, Mocha, and all test runner plugins
- Current `LineMarkerProvider` approach requires manual run configuration creation in the click handler, which doesn't integrate with the standard executor framework

**Alternatives considered**:
- **Keep `LineMarkerProvider`**: Works but doesn't provide the standard Run/Debug context menu. Requires manual click handling. Not how IntelliJ test runners work.

## Decision 3: Pass/Fail Gutter State

**Decision**: Use `TestStateStorage` to persist task execution results and `ExecutionListener` to track completion.

**Rationale**:
- `TestStateStorage` is the standard IntelliJ mechanism for storing execution results
- `RunLineMarkerContributor.getTestStateIcon()` reads from `TestStateStorage` automatically
- `ExecutionListener` on `ExecutionManager.EXECUTION_TOPIC` provides `processTerminated` with exit code
- Icons update automatically on next editor reparse after state change
- Reset on file edit is handled by invalidating the stored state when documents change

**Alternatives considered**:
- **`RangeHighlighter` via `MarkupModel`**: Lower-level, more dynamic, but doesn't integrate with `RunLineMarkerContributor`'s built-in state icon support. More code, more bugs.
- **Custom project service**: Would reinvent what `TestStateStorage` already provides.

## Decision 4: Language Server Discovery

**Decision**: Search project's `node_modules/@nadle/language-server/server.mjs` first, then fall back to global `npx @nadle/language-server` resolution.

**Rationale**:
- Mirrors the VS Code extension pattern (copies server into extension, resolves from local path)
- Project-local resolution ensures version consistency with the project's dependencies
- Global fallback covers the case where the language server is installed globally
- The binary entry point is `server.mjs` with `#!/usr/bin/env node` shebang
- Server accepts no arguments — `--stdio` is injected internally by `server.mjs`

**Alternatives considered**:
- **Bundle server in plugin JAR**: Would bloat the plugin and create version sync issues. The server updates independently with the nadle package.
- **Manual path configuration**: Adds friction. The spec explicitly says "no manual path configuration required."

## Decision 5: Platform Target

**Decision**: Change `platformType` from `IC` (Community) to `IU` (Ultimate) with LSP as an optional module dependency. Keep `JavaScript` as a required dependency.

**Rationale**:
- The built-in LSP API (`com.intellij.platform.lsp`) is not available in Community Edition
- TypeScript developers using Nadle typically use WebStorm or IntelliJ Ultimate
- Using `<depends optional="true" config-file="nadle-lsp.xml">com.intellij.modules.lsp</depends>` allows the plugin to work without LSP features on IDEs that don't have the module
- Gutter play buttons and task execution work on all editions regardless

**Alternatives considered**:
- **Stay on `IC`**: Cannot use built-in LSP API. Would require LSP4IJ as a dependency.
- **Target `WS` (WebStorm only)**: Too restrictive. Many TypeScript devs use IntelliJ Ultimate with the JavaScript plugin.

## Decision 6: Debug Implementation

**Decision**: Debug mode runs `node --inspect-brk` with the nadle binary, allowing IntelliJ's Node.js debugger to attach.

**Rationale**:
- `--inspect-brk` pauses execution at start, giving the debugger time to attach
- IntelliJ's built-in Node.js debug support handles the V8 debug protocol
- Matches how Vitest/Mocha debug works in IntelliJ
- The `NadleTaskRunConfiguration` will create a `NodeCommandLineConfigurator` or equivalent when debug executor is used

**Alternatives considered**:
- **`--inspect` (no break)**: Risk of missing early breakpoints since the task starts immediately.
- **No debug**: User explicitly requested debug support.

## Decision 7: File Pattern Matching

**Decision**: Support all nadle config file extensions (`nadle.config.{ts,js,mts,mjs,cts,cjs}`) for both LSP and gutter icons.

**Rationale**:
- The language server already supports this pattern: `/^nadle\.config\.[cm]?[jt]s$/`
- Current plugin only checks `nadle.config.ts` — too restrictive
- The VS Code extension supports all variants via document selector
- Must register `RunLineMarkerContributor` for both TypeScript and JavaScript languages

**Alternatives considered**:
- **TypeScript only**: Would miss `.js`, `.mjs`, `.cjs` config files that the language server supports.

## Decision 8: Plugin.xml Registration Fixes

**Decision**: Explicitly register all extension points in `plugin.xml` instead of relying on SPI auto-discovery.

**Rationale**:
- Current plugin is missing `configurationType` and `runConfigurationProducer` registrations
- SPI auto-discovery is unreliable and not the recommended approach
- Explicit registration ensures all features work correctly
- New registrations needed: `platform.lsp.serverSupportProvider`, `runLineMarkerContributor`, `configurationType`, `runConfigurationProducer`
- Remove old `lineMarkerProvider` registration

**Alternatives considered**:
- **Keep SPI auto-discovery**: May work but is fragile and not documented as the primary registration method.
