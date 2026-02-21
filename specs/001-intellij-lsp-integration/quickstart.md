# Quickstart: IntelliJ LSP Integration for Nadle

**Date**: 2026-02-21
**Branch**: `001-intellij-lsp-integration`

## Prerequisites

- IntelliJ IDEA Ultimate 2024.2+ or WebStorm 2024.2+
- JDK 21 (for plugin development)
- Node.js 22+ (for running the language server)
- Gradle 8.13 (included via wrapper)

## Setup for Development

```bash
# Clone and checkout feature branch
git clone https://github.com/nadlejs/intellij-plugin.git
cd intellij-plugin
git checkout 001-intellij-lsp-integration

# Run the plugin in a sandbox IDE
./gradlew runIde
```

## Testing with a Nadle Project

1. Create or open a project with `@nadle/language-server` installed:
   ```bash
   mkdir test-project && cd test-project
   pnpm init
   pnpm add -D nadle @nadle/language-server
   ```

2. Create a `nadle.config.ts` file:
   ```typescript
   import { tasks, ExecTask } from "nadle";

   tasks.register("build", ExecTask, {
     command: "tsc",
     args: ["--build"]
   }).config({
     description: "Compile TypeScript",
     group: "Building"
   });

   tasks.register("test", ExecTask, {
     command: "vitest",
     args: ["run"]
   }).config({
     description: "Run tests",
     dependsOn: ["build"]
   });

   tasks.register("lint", ExecTask, {
     command: "eslint",
     args: ["."]
   }).config({
     description: "Lint source code",
     group: "Verification"
   });
   ```

3. Open the project in the sandbox IDE launched by `./gradlew runIde`

## Verifying Features

### Gutter Play Buttons (FR-007, FR-008)
- Open `nadle.config.ts` — each `tasks.register()` line should have a ▶ icon
- Click the ▶ icon — context menu with "Run" and "Debug" should appear
- Select "Run" — task executes in the Run tool window

### Diagnostics (FR-002)
- Change a task name to `"123bad"` — error underline should appear
- Duplicate a task name — error should flag the duplicate
- Reference a non-existent dependency — warning should appear

### Completions (FR-003)
- Inside a `.config({ dependsOn: ["` trigger autocomplete
- Available task names should appear in the popup

### Hover (FR-004)
- Hover over a task name string — metadata popup should display

### Go-to-Definition (FR-005)
- Ctrl+Click on a task name in `dependsOn` — cursor should jump to its `tasks.register()` call

## Project Structure (After Implementation)

```
src/main/kotlin/com/github/nadlejs/intellij/plugin/
├── NadleTaskRunLineMarkerContributor.kt  # Gutter play buttons (replaces LineMarkerProvider)
├── NadleTaskConfigurationType.kt          # Run configuration type definition
├── NadleTaskRunConfiguration.kt           # Run configuration with run/debug support
├── NadleTaskConfigurationProducer.kt      # Context-aware configuration creation
├── NadleTaskSettingsEditor.kt             # Run configuration UI editor
├── NadleTaskExecutionListener.kt          # Tracks task pass/fail for gutter state
├── NadleLspServerSupportProvider.kt       # LSP server lifecycle management
├── NadleLspServerDescriptor.kt            # LSP server startup and file matching
└── NadleFileUtil.kt                       # Shared file pattern matching utility

src/main/resources/META-INF/
├── plugin.xml                             # Main plugin manifest
└── nadle-lsp.xml                          # Optional LSP extension points
```

## Build and Package

```bash
# Build the plugin
./gradlew buildPlugin

# Run tests
./gradlew check

# Package for distribution
./gradlew buildPlugin
# Output: build/distributions/nadle-*.zip
```
