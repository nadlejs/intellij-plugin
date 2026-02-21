# Plugin Extension Points Contract

**Date**: 2026-02-21

## plugin.xml Registration

### Required Extensions

```xml
<idea-plugin>
    <id>com.github.nadlejs.nadle</id>
    <name>Nadle Task Runner</name>
    <vendor url="https://github.com/nadlejs">Nadle</vendor>

    <depends>com.intellij.modules.platform</depends>
    <depends>JavaScript</depends>
    <!-- Optional: LSP features degrade gracefully without this -->
    <depends optional="true"
             config-file="nadle-lsp.xml">com.intellij.modules.lsp</depends>

    <extensions defaultExtensionNs="com.intellij">
        <!-- Gutter play buttons (TypeScript + JavaScript) -->
        <runLineMarkerContributor language="TypeScript"
            implementationClass="...NadleTaskRunLineMarkerContributor"/>
        <runLineMarkerContributor language="JavaScript"
            implementationClass="...NadleTaskRunLineMarkerContributor"/>

        <!-- Run configuration type -->
        <configurationType
            implementation="...NadleTaskConfigurationType"/>

        <!-- Context-aware run configuration creation -->
        <runConfigurationProducer
            implementation="...NadleTaskConfigurationProducer"/>
    </extensions>
</idea-plugin>
```

### LSP Extensions (nadle-lsp.xml — loaded only when LSP module available)

```xml
<idea-plugin>
    <extensions defaultExtensionNs="com.intellij">
        <platform.lsp.serverSupportProvider
            implementation="...NadleLspServerSupportProvider"/>
    </extensions>
</idea-plugin>
```

## Extension Point Mapping to Requirements

| Extension Point | Class | Requirements |
| --------------- | ----- | ------------ |
| `runLineMarkerContributor` | `NadleTaskRunLineMarkerContributor` | FR-007, FR-010 |
| `configurationType` | `NadleTaskConfigurationType` | FR-013 |
| `runConfigurationProducer` | `NadleTaskConfigurationProducer` | FR-008, FR-009, FR-011 |
| `platform.lsp.serverSupportProvider` | `NadleLspServerSupportProvider` | FR-001, FR-002, FR-003, FR-004, FR-005, FR-006, FR-015 |

## Removed Extensions

| Old Extension | Reason |
| ------------- | ------ |
| `lineMarkerProvider` (`NadleTaskLineMarkerProvider`) | Replaced by `runLineMarkerContributor` for proper Run/Debug context menu |
