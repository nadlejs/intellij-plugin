package com.github.nadlejs.intellij.plugin.run

import com.github.nadlejs.intellij.plugin.util.NadleIcons
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.openapi.project.Project
import javax.swing.Icon

class NadleTaskConfigurationType : ConfigurationType {

	override fun getDisplayName(): String = "Nadle"

	override fun getConfigurationTypeDescription(): String = "Nadle task runner configuration"

	override fun getIcon(): Icon = NadleIcons.Nadle

	override fun getId(): String = "NADLE_TASK_CONFIGURATION"

	override fun getConfigurationFactories(): Array<ConfigurationFactory> =
		arrayOf(NadleTaskConfigurationFactory(this))

	private class NadleTaskConfigurationFactory(type: ConfigurationType) : ConfigurationFactory(type) {

		override fun createTemplateConfiguration(project: Project): RunConfiguration =
			NadleTaskRunConfiguration(project, this, "Nadle")

		override fun getId(): String = "NADLE_TASK_FACTORY"
	}
}