package com.github.nadlejs.intellij.plugin.run

import com.intellij.execution.RunManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.openapi.project.Project

object NadleTaskRunner {

	fun run(project: Project, task: NadleTask) {
		val settings = findOrCreateConfiguration(project, task)
		RunManager.getInstance(project).selectedConfiguration = settings
		ExecutionUtil.runConfiguration(
			settings,
			DefaultRunExecutor.getRunExecutorInstance()
		)
	}

	fun debug(project: Project, task: NadleTask) {
		val settings = findOrCreateConfiguration(project, task)
		RunManager.getInstance(project).selectedConfiguration = settings
		ExecutionUtil.runConfiguration(
			settings,
			DefaultDebugExecutor.getDebugExecutorInstance()
		)
	}

	fun createRunConfiguration(
		project: Project,
		task: NadleTask
	): RunnerAndConfigurationSettings {
		val runManager = RunManager.getInstance(project)
		val configType = ConfigurationTypeUtil.findConfigurationType(
			NadleTaskConfigurationType::class.java
		)
		val factory = configType.configurationFactories[0]
		val settings = runManager.createConfiguration(task.name, factory)
		val config = settings.configuration as NadleTaskRunConfiguration
		config.taskName = task.name
		config.configFilePath = task.configFilePath.toString()
		config.workingDirectory = task.workingDirectory.toString()
		settings.isTemporary = false
		runManager.addConfiguration(settings)
		return settings
	}

	private fun findOrCreateConfiguration(
		project: Project,
		task: NadleTask
	): RunnerAndConfigurationSettings {
		val runManager = RunManager.getInstance(project)

		val existing = runManager.allSettings.find { settings ->
			val config = settings.configuration
			config is NadleTaskRunConfiguration
				&& config.taskName == task.name
				&& config.configFilePath == task.configFilePath.toString()
		}

		if (existing != null) return existing

		val configType = ConfigurationTypeUtil.findConfigurationType(
			NadleTaskConfigurationType::class.java
		)
		val factory = configType.configurationFactories[0]
		val settings = runManager.createConfiguration(task.name, factory)
		val config = settings.configuration as NadleTaskRunConfiguration
		config.taskName = task.name
		config.configFilePath = task.configFilePath.toString()
		config.workingDirectory = task.workingDirectory.toString()
		settings.isTemporary = true
		runManager.addConfiguration(settings)
		return settings
	}
}
