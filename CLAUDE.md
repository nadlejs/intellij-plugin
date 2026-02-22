# Nadle IntelliJ Plugin

## Tech Stack
- Kotlin 2.1.20, JVM 21
- IntelliJ Platform SDK 2024.2.5
- IntelliJ Platform Gradle Plugin 2.5.0
- JavaScript plugin dependency

## Project Structure

```text
src/main/kotlin/com/github/nadlejs/intellij/plugin/
  run/          # Run configurations, execution, gutter markers
  lsp/          # Language Server Protocol integration
  structure/    # File Structure popup (Cmd+F12)
  util/         # Shared utilities (file detection, icons, Node.js resolver)
src/main/resources/
  META-INF/plugin.xml    # Extension point registrations
  messages/MyBundle.properties
  icons/nadle.svg
```

## Commands

```bash
./gradlew compileKotlin   # Check compilation
./gradlew build           # Full build
./gradlew runIde          # Launch sandboxed IDE
```

## Key Patterns
- `NadleFileUtil.isNadleConfigFile()` guards all extension points to nadle config files only
- `NadleFileUtil.TASK_REGISTER_PATTERN` regex extracts task names from `tasks.register()` calls
- `NodeJsResolver` resolves Node.js binary across macOS GUI, shell PATH, and well-known paths
- Run configurations use `npx nadle <taskName>` for execution, direct node for debug

<!-- MANUAL ADDITIONS START -->
<!-- MANUAL ADDITIONS END -->
