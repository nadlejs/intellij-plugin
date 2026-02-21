# Data Model: IntelliJ LSP Integration for Nadle Language Server

**Date**: 2026-02-21
**Branch**: `001-intellij-lsp-integration`

## Entities

### NadleConfigFile

Represents a Nadle configuration file detected in the project.

| Attribute | Type | Description |
| --------- | ---- | ----------- |
| virtualFile | VirtualFile | The IntelliJ virtual file reference |
| fileName | String | File name matching `nadle.config.{ts,js,mts,mjs,cts,cjs}` |
| projectPath | String | Absolute path to the project root containing this config |

**Validation**: File name must match regex `/^nadle\.config\.[cm]?[jt]s$/`

### TaskRegistration

A task defined via `tasks.register("name", ...)` in a nadle config file. Detected by the language server and used by the gutter icon system.

| Attribute | Type | Description |
| --------- | ---- | ----------- |
| name | String | Task name (must match `/^[a-z]([a-z0-9-]*[a-z0-9])?$/i`) |
| form | Enum | `no-op`, `function`, or `typed` |
| taskObjectName | String? | For typed form: the task class name (e.g., `ExecTask`) |
| description | String? | Human-readable description from `.config()` |
| group | String? | Task group from `.config()` |
| dependsOn | List<String> | Task dependencies from `.config()` |
| hasInputs | Boolean | Whether task declares inputs |
| hasOutputs | Boolean | Whether task declares outputs |
| lineNumber | Int | Line number in the source file |
| filePath | String | Path to the containing nadle config file |

### TaskExecutionState

Tracks the last execution result for a task, used to update gutter icons.

| Attribute | Type | Description |
| --------- | ---- | ----------- |
| taskName | String | The task name |
| filePath | String | Path to the nadle config file |
| status | Enum | `not_run`, `running`, `passed`, `failed` |
| exitCode | Int? | Process exit code (null if not run or running) |
| timestamp | Long | Epoch millis of last state change |

**State Transitions**:
```
not_run → running → passed (exit code 0)
not_run → running → failed (exit code != 0)
passed  → not_run (on file edit)
failed  → not_run (on file edit)
passed  → running → passed/failed (on re-run)
failed  → running → passed/failed (on re-run)
```

### NadleRunConfiguration

Persisted run configuration for executing a Nadle task.

| Attribute | Type | Description |
| --------- | ---- | ----------- |
| taskName | String | The task to execute |
| workingDirectory | String | Project base path |
| debugMode | Boolean | Whether to run with `--inspect-brk` |

## Relationships

```
NadleConfigFile 1──* TaskRegistration
    "A config file contains zero or more task registrations"

TaskRegistration 1──1 TaskExecutionState
    "Each task has at most one execution state (last run result)"

TaskRegistration 1──1 NadleRunConfiguration
    "Each task maps to a reusable run configuration"

TaskRegistration *──* TaskRegistration
    "Tasks reference each other via dependsOn (within same file)"
```

## LSP Protocol Data (External — from Language Server)

The language server produces and consumes these LSP protocol structures. The plugin does not manage them directly — IntelliJ's LSP client handles the protocol mapping.

| LSP Feature | Request | Response |
| ----------- | ------- | -------- |
| Diagnostics | (pushed by server) | `Diagnostic[]` with codes: `nadle/invalid-task-name`, `nadle/duplicate-task-name`, `nadle/unresolved-dependency` |
| Completions | `textDocument/completion` | `CompletionItem[]` with label (task name), kind (Value), detail (form + description) |
| Hover | `textDocument/hover` | Markdown content with task metadata |
| Definition | `textDocument/definition` | `Location` pointing to the `tasks.register()` call |
