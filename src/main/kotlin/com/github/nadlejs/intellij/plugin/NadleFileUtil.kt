package com.github.nadlejs.intellij.plugin

import com.intellij.openapi.vfs.VirtualFile

object NadleFileUtil {

	private val NADLE_CONFIG_PATTERN = Regex("""^nadle\.config\.[cm]?[jt]s$""")

	val TASK_REGISTER_PATTERN = Regex("""tasks\.register\s*\(\s*['"]([^'"]+)['"]""")

	fun isNadleConfigFile(file: VirtualFile): Boolean =
		NADLE_CONFIG_PATTERN.matches(file.name)

	fun extractTaskName(text: String): String? =
		TASK_REGISTER_PATTERN.find(text)?.groupValues?.get(1)
}
