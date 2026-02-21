package com.github.nadlejs.intellij.plugin

import com.intellij.execution.lineMarker.ExecutorAction
import com.intellij.execution.lineMarker.RunLineMarkerContributor
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import javax.swing.Icon

class NadleTaskRunLineMarkerContributor : RunLineMarkerContributor() {

	override fun getInfo(element: PsiElement): Info? {
		val file = element.containingFile ?: return null
		val virtualFile = file.virtualFile ?: return null

		if (!NadleFileUtil.isNadleConfigFile(virtualFile)) {
			return null
		}

		val elementText = element.text ?: return null
		val taskName = NadleFileUtil.extractTaskName(elementText) ?: return null

		// Only process elements large enough to contain the full pattern
		// but avoid processing the entire file PSI root
		if (element.parent?.parent == null || element == file) {
			return null
		}

		val icon = getTaskStateIcon(virtualFile.path, taskName, element.project)
		val actions = ExecutorAction.getActions(0)

		return Info(icon, actions) { "Run Nadle task: $taskName" }
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
