# Quickstart: Task Explorer Panel

## Prerequisites

- JDK 21
- IntelliJ IDEA (for development)

## Build & Run

```bash
# Compile to check for errors
./gradlew compileKotlin

# Launch sandboxed IDE with the plugin
./gradlew runIde
```

## Verify the Feature

1. Open a project containing `nadle.config.ts` (or any `nadle.config.*` variant)
2. Click the **Nadle** tool window icon in the right sidebar
3. Verify the tree shows config files as parent nodes with tasks underneath
4. Double-click a task → it should run in the Run tool window
5. Right-click a task → context menu should show Run, Debug, Create Run Configuration
6. Edit the config file to add/remove a `tasks.register()` call → tree should auto-refresh
7. Click the Refresh button in the toolbar → tree should rebuild
8. Run a task, then check that pass/fail icons appear on the task node

## Key Files

| File | Purpose |
|------|---------|
| `toolwindow/NadleToolWindowFactory.kt` | Extension point entry — creates panel |
| `toolwindow/NadleToolWindowPanel.kt` | Tree building, actions, refresh logic |
| `run/NadleTaskRunner.kt` | Shared task execution utility |
| `plugin.xml` | `<toolWindow>` registration |
