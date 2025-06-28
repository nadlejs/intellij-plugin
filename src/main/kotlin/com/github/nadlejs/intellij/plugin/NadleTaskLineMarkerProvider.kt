package com.github.nadlejs.intellij.plugin

import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.daemon.LineMarkerProvider
import com.intellij.codeInsight.daemon.GutterIconNavigationHandler
import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.psi.PsiElement
import com.intellij.util.ui.UIUtil
import com.jediterm.core.input.MouseEvent
import kotlin.collections.get

class NadleTaskLineMarkerProvider : LineMarkerProvider {

	companion object {
		private val TASK_REGISTER_PATTERN = Regex("""tasks\.register\s*\(\s*['"]([^'"]+)['"]""")
	}

	override fun getLineMarkerInfo(element: PsiElement): LineMarkerInfo<*>? {
		// Only process files named nadle.config.ts
		val file = element.containingFile ?: return null
		val virtualFile = file.virtualFile ?: return null

		if (virtualFile.name != "nadle.config.ts") {
			return null
		}

		// Check if this element represents a line with tasks.register
		val elementText = element.text ?: return null
		val matchResult = TASK_REGISTER_PATTERN.find(elementText) ?: return null
		val taskName = matchResult.groupValues[1]

		return LineMarkerInfo(
			element,
			element.textRange,
			AllIcons.RunConfigurations.TestState.Run,
			{ "Run Nadle task: $taskName" },
			NadleGutterIconNavigationHandler(taskName),
			GutterIconRenderer.Alignment.CENTER,
			{ "Run Nadle task: $taskName" }
		)
	}

	private class NadleGutterIconNavigationHandler(
		private val taskName: String
	) : GutterIconNavigationHandler<PsiElement> {

		override fun navigate(p0: java.awt.event.MouseEvent?, psiElement: PsiElement?) {
			val project = psiElement?.project ?: return

			// Create and execute run configuration
			val runManager = RunManager.getInstance(project)

			// Try to find existing configuration
			var existingConfig = findExistingConfiguration(runManager, taskName)

			if (existingConfig == null) {
				// Create new configuration
				val configurationType = NadleTaskConfigurationType()
				existingConfig = runManager.createConfiguration(
					"Nadle: $taskName",
					configurationType.configurationFactories[0]
				)

				val runConfig = existingConfig.configuration as NadleTaskRunConfiguration
				runConfig.taskName = taskName

				runManager.addConfiguration(existingConfig)
			}

			// Execute the configuration
			ExecutionUtil.runConfiguration(existingConfig, DefaultRunExecutor.getRunExecutorInstance())
		}

		private fun findExistingConfiguration(runManager: RunManager, taskName: String): RunnerAndConfigurationSettings? {
			return runManager.allSettings.find { config ->
				val configuration = config.configuration
				configuration is NadleTaskRunConfiguration && configuration.taskName == taskName
			}
		}
	}
}