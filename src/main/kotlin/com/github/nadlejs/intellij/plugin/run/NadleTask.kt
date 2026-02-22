package com.github.nadlejs.intellij.plugin.run

import java.nio.file.Path

data class NadleTask(
	val name: String,
	val configFilePath: Path,
	val workingDirectory: Path
)
