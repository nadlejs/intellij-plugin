package com.github.nadlejs.intellij.plugin

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

class NadleTaskConfigurationProducer : LazyRunConfigurationProducer<NadleTaskRunConfiguration>() {

	companion object {
		private val TASK_REGISTER_PATTERN = Regex("""tasks\.register\s*\(\s*['"]([^'"]+)['"]""")
	}

	override fun getConfigurationFactory(): ConfigurationFactory =
		NadleTaskConfigurationType().configurationFactories[0]

	override fun setupConfigurationFromContext(
		configuration: NadleTaskRunConfiguration,
		context: ConfigurationContext,
		sourceElement: Ref<PsiElement>
	): Boolean {
		val element = context.psiLocation ?: return false
		val file = element.containingFile ?: return false
		val virtualFile = file.virtualFile ?: return false

		if (virtualFile.name != "nadle.config.ts") {
			return false
		}

		val elementText = element.text ?: return false
		val matchResult = TASK_REGISTER_PATTERN.find(elementText) ?: return false
		val taskName = matchResult.groupValues[1]

		configuration.taskName = taskName
		configuration.name = "Nadle: $taskName"

		return true
	}

	override fun isConfigurationFromContext(
		configuration: NadleTaskRunConfiguration,
		context: ConfigurationContext
	): Boolean {
		val element = context.psiLocation ?: return false
		val elementText = element.text ?: return false
		val matchResult = TASK_REGISTER_PATTERN.find(elementText) ?: return false
		val taskName = matchResult.groupValues[1]

		return taskName == configuration.taskName
	}
}