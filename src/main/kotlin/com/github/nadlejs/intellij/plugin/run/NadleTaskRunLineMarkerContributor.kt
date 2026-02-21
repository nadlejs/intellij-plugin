package com.github.nadlejs.intellij.plugin.run

import com.github.nadlejs.intellij.plugin.util.NadleFileUtil
import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import javax.swing.Icon

class NadleTaskRunLineMarkerContributor : RunLineMarkerContributor() {

	override fun getInfo(element: PsiElement): Info? {
		// Only process leaf elements to produce exactly one icon per call
		if (element.firstChild != null) return null

		val file = element.containingFile ?: return null
		val virtualFile = file.virtualFile ?: return null

		if (!NadleFileUtil.isNadleConfigFile(virtualFile)) return null

		// Anchor on the "tasks" identifier leaf to get one icon per
		// tasks.register() call, placed at the start of the expression
		if (element.text != "tasks") return null

		val callText = findEnclosingCallText(element) ?: return null
		val taskName = NadleFileUtil.extractTaskName(callText) ?: return null

		val icon = getTaskStateIcon(virtualFile.path, taskName, element.project)
		val actions = ExecutorAction.getActions(0)

		return Info(icon, actions) { "Run Nadle task: $taskName" }
	}

	private fun findEnclosingCallText(element: PsiElement): String? {
		var current = element.parent
		var depth = 0
		while (current != null && depth < 10) {
			val text = current.text
			if (NadleFileUtil.TASK_REGISTER_PATTERN.containsMatchIn(text)) {
				return text
			}
			current = current.parent
			depth++
		}
		return null
	}

	companion object {

		private fun getTaskStateIcon(
			filePath: String,
			taskName: String,
			project: Project
		): Icon {
			val result = NadleTaskStateService.getInstance(project)
				.getResult(filePath, taskName)

			return when (result) {
				NadleTaskStateService.TaskResult.PASSED ->
					AllIcons.RunConfigurations.TestState.Green2
				NadleTaskStateService.TaskResult.FAILED ->
					AllIcons.RunConfigurations.TestState.Red2
				null ->
					AllIcons.RunConfigurations.TestState.Run
			}
		}
	}
}
