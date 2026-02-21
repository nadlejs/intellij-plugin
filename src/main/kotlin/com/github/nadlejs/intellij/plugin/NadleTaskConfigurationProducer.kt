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

		val taskName = findTaskName(element) ?: return false

		configuration.taskName = taskName
		configuration.configFilePath = virtualFile.path
		configuration.name = taskName

		return true
	}

	override fun isConfigurationFromContext(
		configuration: NadleTaskRunConfiguration,
		context: ConfigurationContext
	): Boolean {
		val element = context.psiLocation ?: return false
		val taskName = findTaskName(element) ?: return false

		return taskName == configuration.taskName
	}

	private fun findTaskName(element: PsiElement): String? {
		// Try the element itself first
		NadleFileUtil.extractTaskName(element.text ?: return null)
			?.let { return it }

		// Walk up the PSI tree to find enclosing tasks.register() call
		var current = element.parent
		var depth = 0
		while (current != null && depth < 10) {
			val text = current.text
			NadleFileUtil.extractTaskName(text)?.let { return it }
			current = current.parent
			depth++
		}
		return null
	}
}
