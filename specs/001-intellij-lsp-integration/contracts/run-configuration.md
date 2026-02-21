# Run Configuration Contract

**Date**: 2026-02-21

## Configuration Type

| Property | Value |
| -------- | ----- |
| ID | `NADLE_TASK_CONFIGURATION` |
| Display Name | `Nadle Task` |
| Icon | `AllIcons.RunConfigurations.TestState.Run` |
| Factory ID | `NADLE_TASK_FACTORY` |

## Configuration Options

| Field | Type | Required | Default | Description |
| ----- | ---- | -------- | ------- | ----------- |
| taskName | String | Yes | (empty) | Name of the nadle task to execute |
| workingDirectory | String | No | Project base path | Working directory for execution |

## Execution Commands

### Run Mode

```bash
npx nadle <taskName>
```

Working directory: project base path (or configured working directory).

### Debug Mode

```bash
node --inspect-brk ./node_modules/@nadle/language-server/../nadle/bin/nadle.mjs <taskName>
```

Or equivalently, using the nadle binary with Node.js inspect:

```bash
node --inspect-brk $(which npx) nadle <taskName>
```

The debug executor attaches IntelliJ's Node.js debugger to the V8 inspect port.

## Gutter Icon State URL Scheme

Task execution results are stored in `TestStateStorage` using this URL format:

```
nadle:task://<filePath>#<taskName>
```

Example: `nadle:task:///Users/dev/project/nadle.config.ts#build`

### State Mapping

| Exit Code | TestStateStorage Magnitude | Gutter Icon |
| --------- | -------------------------- | ----------- |
| 0 | `PASSED_INDEX` | Green checkmark (✓) |
| non-zero | `FAILED_INDEX` | Red cross (✗) |
| (not run) | (no entry) | Green play button (▶) |

### State Reset Trigger

When a nadle config file is modified (document change event), all `TestStateStorage` entries with that file's path prefix are cleared, resetting all gutter icons to the default play button.

## ConfigurationProducer Contract

### Context Detection

The producer activates when:
1. The PSI element is inside a file matching `nadle.config.{ts,js,mts,mjs,cts,cjs}`
2. The element text matches `tasks.register("name", ...)`

### Configuration Reuse

`isConfigurationFromContext()` returns `true` when an existing configuration's `taskName` matches the task name extracted from the current PSI context. This prevents duplicate configurations.
