# LSP Server Lifecycle Contract

**Date**: 2026-02-21

## Server Discovery

### Resolution Order

1. **Project-local**: `<projectRoot>/node_modules/@nadle/language-server/server.mjs`
2. **Global fallback**: Resolve via `which nadle-language-server` or `npx @nadle/language-server`
3. **Not found**: Log warning, skip LSP initialization. Plugin continues without language intelligence.

### Prerequisites

- Node.js must be installed and accessible in PATH
- `@nadle/language-server` package must be installed (project-local or global)

## Server Startup

### Command Line

```bash
node <resolved-path-to-server.mjs>
```

The `server.mjs` entry point internally injects `--stdio` to `process.argv` before importing the server module. No additional arguments needed from the plugin side.

### Transport

- **Protocol**: Language Server Protocol (LSP) over stdio
- **Encoding**: JSON-RPC 2.0 over stdin/stdout
- **Sync Mode**: `TextDocumentSyncKind.Incremental` (value: 2)

## Server Capabilities (from InitializeResult)

```json
{
    "capabilities": {
        "textDocumentSync": {
            "openClose": true,
            "change": 2,
            "save": { "includeText": false }
        },
        "completionProvider": {
            "triggerCharacters": ["\"", "'"],
            "resolveProvider": false
        },
        "hoverProvider": true,
        "definitionProvider": true
    }
}
```

## File Activation Pattern

Server processes files matching: `/^nadle\.config\.[cm]?[jt]s$/`

Concrete file names:
- `nadle.config.ts`
- `nadle.config.js`
- `nadle.config.mts`
- `nadle.config.mjs`
- `nadle.config.cts`
- `nadle.config.cjs`

## Error Handling

| Scenario | Behavior |
| -------- | -------- |
| Node.js not found | Log warning, skip LSP. Gutter icons still work. |
| Server binary not found | Log warning, skip LSP. Gutter icons still work. |
| Server crashes during operation | IntelliJ's LSP client handles restart automatically. |
| Server returns invalid response | IntelliJ's LSP client logs and ignores the response. |
