# Nadle Task Runner for IntelliJ

![Build](https://github.com/nadlejs/intellij-plugin/workflows/Build/badge.svg)

<!-- Plugin description -->
Integrates [Nadle](https://github.com/nadlejs/nadle) task execution into IntelliJ IDEA.

**Features:**
- Run icons next to `tasks.register("name", fn)` definitions for one-click execution
- Language intelligence (diagnostics, completions, hover, go-to-definition) for `nadle.config.*` files via the bundled Nadle Language Server
- Navigate from `dependsOn` references to task definitions
- File Structure popup (Cmd+F12) lists all registered tasks with filtering
- Task name autocomplete in run configuration editor with monorepo support
- Debug support with Node.js inspector
- Works with `nadle.config.ts`, `nadle.config.js`, and other supported extensions
<!-- Plugin description end -->

## Installation

### From JetBrains Marketplace

<kbd>Settings</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > Search for **"Nadle"** > <kbd>Install</kbd>

### From Disk

Download the [latest release](https://github.com/nadlejs/intellij-plugin/releases/latest) and install manually:

<kbd>Settings</kbd> > <kbd>Plugins</kbd> > <kbd>&#9881;</kbd> > <kbd>Install plugin from disk...</kbd>

## Usage

1. Open a project containing `nadle.config.ts` (or `.js`, `.mjs`, `.cjs`, `.mts`, `.cts`)
2. Click the run icon in the gutter next to any `tasks.register()` call
3. Press <kbd>Cmd+F12</kbd> to browse all tasks in the File Structure popup
4. Use <kbd>Ctrl+Space</kbd> in the run configuration editor for task name autocomplete

## Requirements

- IntelliJ IDEA 2024.2+
- Node.js (for task execution and language server)
- [Nadle](https://github.com/nadlejs/nadle) installed as a project dependency

## Development

```bash
# Build the plugin
./gradlew build

# Run a sandboxed IDE instance with the plugin
./gradlew runIde

# Check compilation
./gradlew compileKotlin
```
