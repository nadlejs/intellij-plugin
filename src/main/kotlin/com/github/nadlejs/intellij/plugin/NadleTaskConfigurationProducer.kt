package com.github.nadlejs.intellij.plugin

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.LazyRunConfigurationProducer
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement

class NadleTaskConfigurationProducer : LazyRunConfigurationProducer<NadleTaskRunConfiguration>() {

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

		if (!NadleFileUtil.isNadleConfigFile(virtualFile)) {
			return false
		}

		val elementText = element.text ?: return false
		val taskName = NadleFileUtil.extractTaskName(elementText) ?: return false

		configuration.taskName = taskName
		configuration.configFilePath = virtualFile.path
		configuration.name = "Nadle: $taskName"

		return true
	}

	override fun isConfigurationFromContext(
		configuration: NadleTaskRunConfiguration,
		context: ConfigurationContext
	): Boolean {
		val element = context.psiLocation ?: return false
		val elementText = element.text ?: return false
		val taskName = NadleFileUtil.extractTaskName(elementText) ?: return false

		return taskName == configuration.taskName
	}
}
