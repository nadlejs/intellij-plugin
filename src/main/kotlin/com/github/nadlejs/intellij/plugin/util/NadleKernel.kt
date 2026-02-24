package com.github.nadlejs.intellij.plugin.util

data class TaskReference(
	val taskName: String,
	val workspaceInput: String?
)

object NadleKernel {

	const val ROOT_WORKSPACE_ID = "root"
	const val COLON = ":"
	private const val SLASH = "/"
	private const val BACKSLASH = "\\"
	private const val DOT = "."

	val VALID_TASK_NAME_PATTERN =
		Regex("^[a-z](?:[a-z0-9-]*[a-z0-9])?\$", RegexOption.IGNORE_CASE)

	fun deriveWorkspaceId(relativePath: String): String {
		if (relativePath == DOT) {
			return ROOT_WORKSPACE_ID
		}
		return relativePath.replace(BACKSLASH, SLASH).replace(SLASH, COLON)
	}

	fun isRootWorkspaceId(workspaceId: String): Boolean =
		workspaceId == ROOT_WORKSPACE_ID

	fun parseTaskReference(input: String): TaskReference {
		if (!input.contains(COLON)) {
			return TaskReference(taskName = input, workspaceInput = null)
		}
		val parts = input.split(COLON)
		return TaskReference(
			taskName = parts.last(),
			workspaceInput = parts.dropLast(1).joinToString(COLON)
		)
	}

	fun composeTaskIdentifier(workspaceLabel: String, taskName: String): String {
		if (workspaceLabel.isEmpty()) {
			return taskName
		}
		return (workspaceLabel.split(COLON) + taskName).joinToString(COLON)
	}

	fun isWorkspaceQualified(input: String): Boolean =
		input.contains(COLON)
}
