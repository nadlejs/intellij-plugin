package com.github.nadlejs.intellij.plugin

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.jdom.Element
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

class NadleTaskRunConfiguration(
	project: Project,
	factory: ConfigurationFactory,
	name: String
) : RunConfigurationBase<RunConfigurationOptions>(project, factory, name) {

	var taskName: String = ""
	var configFilePath: String = ""

	override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
		NadleTaskSettingsEditor()

	@Throws(RuntimeConfigurationException::class)
	override fun checkConfiguration() {
		if (taskName.isBlank()) {
			throw RuntimeConfigurationError("Task name must be specified")
		}

		if (!isCommandAvailable("node")) {
			throw RuntimeConfigurationWarning(
				"Node.js not found in PATH. Task execution requires Node.js."
			)
		}
	}

	private fun isCommandAvailable(command: String): Boolean {
		return try {
			val process = ProcessBuilder(command, "--version")
				.redirectErrorStream(true)
				.start()
			process.waitFor() == 0
		} catch (e: Exception) {
			false
		}
	}

	@Throws(ExecutionException::class)
	override fun getState(
		executor: Executor,
		environment: ExecutionEnvironment
	): RunProfileState {
		val isDebug = executor.id == DefaultDebugExecutor.EXECUTOR_ID
		return NadleTaskRunProfileState(environment, this, isDebug)
	}

	@Throws(RuntimeConfigurationException::class)
	override fun writeExternal(element: Element) {
		super.writeExternal(element)
		element.setAttribute("taskName", taskName)
		element.setAttribute("configFilePath", configFilePath)
	}

	override fun readExternal(element: Element) {
		super.readExternal(element)
		taskName = element.getAttributeValue("taskName") ?: ""
		configFilePath = element.getAttributeValue("configFilePath") ?: ""
	}

	private class NadleTaskRunProfileState(
		private val environment: ExecutionEnvironment,
		private val configuration: NadleTaskRunConfiguration,
		private val isDebug: Boolean
	) : RunProfileState {

		@Throws(ExecutionException::class)
		override fun execute(
			executor: Executor,
			runner: ProgramRunner<*>
		): ExecutionResult {
			val handler = startProcess()

			val console = TextConsoleBuilderFactory.getInstance()
				.createBuilder(environment.project).console
			console.attachToProcess(handler)

			return DefaultExecutionResult(console, handler)
		}

		@Throws(ExecutionException::class)
		private fun startProcess(): ProcessHandler {
			val projectBasePath = environment.project.basePath
				?: throw ExecutionException("Project base path not found")

			val commandLine = if (isDebug) {
				buildDebugCommandLine(projectBasePath)
			} else {
				buildRunCommandLine(projectBasePath)
			}

			return try {
				OSProcessHandler(commandLine)
			} catch (e: ExecutionException) {
				throw ExecutionException(
					"Failed to start Nadle task: ${e.message}", e
				)
			}
		}

		private fun buildRunCommandLine(
			projectBasePath: String
		): GeneralCommandLine {
			return GeneralCommandLine().apply {
				exePath = "npx"
				addParameter("nadle")
				addParameter(configuration.taskName)
				workDirectory = File(projectBasePath)
			}
		}

		private fun buildDebugCommandLine(
			projectBasePath: String
		): GeneralCommandLine {
			val nadleBin = resolveNadleBinary(projectBasePath)

			return GeneralCommandLine().apply {
				exePath = "node"
				addParameter("--inspect-brk")
				addParameter(nadleBin)
				addParameter(configuration.taskName)
				workDirectory = File(projectBasePath)
			}
		}

		private fun resolveNadleBinary(projectBasePath: String): String {
			val localBin = Path.of(
				projectBasePath, "node_modules", ".bin", "nadle"
			)
			if (Files.exists(localBin)) {
				return localBin.toString()
			}

			val localPkg = Path.of(
				projectBasePath, "node_modules", "nadle", "bin", "nadle.mjs"
			)
			if (Files.exists(localPkg)) {
				return localPkg.toString()
			}

			throw ExecutionException(
				"Could not find nadle binary. " +
					"Install nadle as a project dependency: pnpm add -D nadle"
			)
		}
	}
}
