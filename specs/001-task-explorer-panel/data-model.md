# Data Model: Task Explorer Panel

## Entities

### NadleTask (existing)

Already defined in `run/NadleTask.kt`. Reused as-is.

| Field            | Type   | Description                              |
|------------------|--------|------------------------------------------|
| `name`           | String | Task name (qualified with `:path:` prefix for nested configs) |
| `configFilePath` | Path   | Absolute path to the nadle config file   |
| `workingDirectory` | Path | Parent directory of the config file      |

### Config File Group (derived, not persisted)

Grouping key for the tree view. Derived from `NadleTask.configFilePath`.

| Attribute       | Type           | Description                                |
|-----------------|----------------|--------------------------------------------|
| `configFilePath` | Path          | Absolute path to the config file           |
| `relativePath`  | String         | Path relative to project root for display  |
| `tasks`         | List<NadleTask> | Tasks registered in this config file      |

### Task State (existing)

Already defined in `NadleTaskStateService`. Reused as-is.

| Field      | Type                               | Description                  |
|------------|-------------------------------------|------------------------------|
| Key        | `"${filePath}#${taskName}"`        | Composite key                |
| Value      | `TaskResult` (PASSED \| FAILED)    | Last execution result        |

## Relationships

```
Project
  └── Config File Group (1..n, grouped by configFilePath)
        └── NadleTask (1..n, tasks within that config)
              └── TaskResult? (0..1, from NadleTaskStateService)
```

## State Transitions

```
Task State:
  [Not Run] ──(execute, exit 0)──→ [Passed]
  [Not Run] ──(execute, exit ≠0)──→ [Failed]
  [Passed]  ──(execute, exit ≠0)──→ [Failed]
  [Failed]  ──(execute, exit 0)──→ [Passed]
  [Passed/Failed] ──(config file changed)──→ [Not Run]
```

## No New Persistence

All data is derived at runtime:
- Tasks: scanned from filesystem by `NadleTaskScanner`
- State: in-memory via `NadleTaskStateService` (project-scoped, cleared on file change)
- Tree: built fresh on each scan, held in `DefaultTreeModel`
