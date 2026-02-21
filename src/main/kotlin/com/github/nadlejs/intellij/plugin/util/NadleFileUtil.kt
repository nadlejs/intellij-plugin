package com.github.nadlejs.intellij.plugin.util

import com.intellij.openapi.vfs.VirtualFile

object NadleFileUtil {

	private val NADLE_CONFIG_PATTERN = Regex("""^nadle\.config\.[cm]?[jt]s$""")

	val TASK_REGISTER_PATTERN = Regex("""tasks\s*\.register\s*\(\s*['"]([^'"]+)['"]""")

	fun isNadleConfigFile(file: VirtualFile): Boolean =
		NADLE_CONFIG_PATTERN.matches(file.name)

	fun extractTaskName(text: String): String? =
		TASK_REGISTER_PATTERN.find(text)?.groupValues?.get(1)

	fun extractAllTaskNames(text: String): List<String> =
		TASK_REGISTER_PATTERN.findAll(text).map { it.groupValues[1] }.toList()
}
