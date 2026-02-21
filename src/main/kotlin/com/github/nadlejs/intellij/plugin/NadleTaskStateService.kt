package com.github.nadlejs.intellij.plugin

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentHashMap

@Service(Service.Level.PROJECT)
class NadleTaskStateService {

	enum class TaskResult { PASSED, FAILED }

	private val results = ConcurrentHashMap<String, TaskResult>()

	fun setResult(filePath: String, taskName: String, result: TaskResult) {
		results[buildKey(filePath, taskName)] = result
	}

	fun getResult(filePath: String, taskName: String): TaskResult? =
		results[buildKey(filePath, taskName)]

	fun clearResultsForFile(filePath: String) {
		val prefix = "$filePath#"
		results.keys.removeAll { it.startsWith(prefix) }
	}

	private fun buildKey(filePath: String, taskName: String): String =
		"$filePath#$taskName"

	companion object {
		fun getInstance(project: Project): NadleTaskStateService =
			project.getService(NadleTaskStateService::class.java)
	}
}
