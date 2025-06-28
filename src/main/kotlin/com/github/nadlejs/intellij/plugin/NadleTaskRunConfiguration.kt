package com.github.nadlejs.intellij.plugin

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.jdom.Element
import java.io.File

class NadleTaskRunConfiguration(
	project: Project,
	factory: ConfigurationFactory,
	name: String
) : RunConfigurationBase<NadleTaskRunConfigurationOptions>(project, factory, name) {

	var taskName: String = ""

	override fun getOptions(): NadleTaskRunConfigurationOptions =
		super.getOptions() as NadleTaskRunConfigurationOptions

	override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
		NadleTaskSettingsEditor()

	@Throws(RuntimeConfigurationException::class)
	override fun checkConfiguration() {
		if (taskName.isBlank()) {
			throw RuntimeConfigurationError("Task name must be specified")
		}
	}

	@Throws(ExecutionException::class)
	override fun getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
		NadleTaskRunProfileState(environment, this)

	@Throws(RuntimeConfigurationException::class)
	override fun writeExternal(element: Element) {
		super.writeExternal(element)
		element.setAttribute("taskName", taskName)
	}

	override fun readExternal(element: Element) {
		super.readExternal(element)
		taskName = element.getAttributeValue("taskName") ?: ""
	}

	private class NadleTaskRunProfileState(
		private val environment: ExecutionEnvironment,
		private val configuration: NadleTaskRunConfiguration
	) : RunProfileState {

		@Throws(ExecutionException::class)
		override fun execute(executor: Executor, runner: ProgramRunner<*>): ExecutionResult? {
			val handler = startProcess()

			val console = TextConsoleBuilderFactory.getInstance().createBuilder(environment.project).console
			console.attachToProcess(handler)

			return DefaultExecutionResult(console, handler)
		}

		@Throws(ExecutionException::class)
		private fun startProcess(): ProcessHandler {
			val projectBasePath = environment.project.basePath
				?: throw ExecutionException("Project base path not found")

			val commandLine = GeneralCommandLine().apply {
				exePath = "npx"
				addParameter("nadle")
				addParameter(configuration.taskName)
				workDirectory = File(projectBasePath)
			}

			return try {
				OSProcessHandler(commandLine)
			} catch (e: ExecutionException) {
				throw ExecutionException("Failed to start Nadle task: ${e.message}", e)
			}		}
	}
}