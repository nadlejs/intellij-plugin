package com.github.nadlejs.intellij.plugin.run

import com.github.nadlejs.intellij.plugin.util.NadleFileUtil
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

		val rawTaskName = findTaskName(element) ?: return false
		val qualifiedName = NadleTaskRunLineMarkerContributor.qualifyTaskName(
			rawTaskName, virtualFile.path, context.project
		)

		configuration.taskName = qualifiedName
		configuration.configFilePath = virtualFile.path
		configuration.name = qualifiedName
		configuration.workingDirectory = context.project.basePath ?: ""

		return true
	}

	override fun isConfigurationFromContext(
		configuration: NadleTaskRunConfiguration,
		context: ConfigurationContext
	): Boolean {
		val element = context.psiLocation ?: return false
		val file = element.containingFile ?: return false
		val virtualFile = file.virtualFile ?: return false
		val rawTaskName = findTaskName(element) ?: return false
		val qualifiedName = NadleTaskRunLineMarkerContributor.qualifyTaskName(
			rawTaskName, virtualFile.path, context.project
		)

		return qualifiedName == configuration.taskName
			&& virtualFile.path == configuration.configFilePath
	}

	private fun findTaskName(element: PsiElement): String? {
		val elementText = element.text ?: return null

		// Try the element itself first
		NadleFileUtil.extractTaskName(elementText)?.let { return it }

		// Walk up the PSI tree to find the nearest enclosing
		// tasks.register() call. Only accept nodes that contain exactly
		// one match — if a parent has multiple registrations, we've
		// walked too far.
		var current = element.parent
		var depth = 0
		while (current != null && depth < 10) {
			val text = current.text
			val allNames = NadleFileUtil.extractAllTaskNames(text)
			if (allNames.size == 1) return allNames[0]
			if (allNames.size > 1) break
			current = current.parent
			depth++
		}

		return null
	}
}
