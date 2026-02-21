package com.github.nadlejs.intellij.plugin

import com.intellij.execution.DefaultExecutionResult
import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionResult
import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.KillableColoredProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.configuration.EnvironmentVariablesData
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.util.execution.ParametersListUtil
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
	var interpreterRef: NodeJsInterpreterRef = NodeJsInterpreterRef.createProjectRef()
	var workingDirectory: String = ""
	var nadleArguments: String = ""
	var envData: EnvironmentVariablesData = EnvironmentVariablesData.DEFAULT

	override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> =
		NadleTaskSettingsEditor(project)

	@Throws(RuntimeConfigurationException::class)
	override fun checkConfiguration() {
		if (taskName.isBlank()) {
			throw RuntimeConfigurationError("Task name must be specified")
		}

		if (NodeJsResolver.resolve(project, interpreterRef) == null) {
			throw RuntimeConfigurationWarning(
				"Node.js not found. Configure it in Settings > Languages & Frameworks > Node.js, " +
					"or ensure it is available in PATH."
			)
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
		element.setAttribute("workingDirectory", workingDirectory)
		element.setAttribute("nadleArguments", nadleArguments)
		element.setAttribute("nodeInterpreter", interpreterRef.referenceName)
		envData.writeExternal(element)
	}

	override fun readExternal(element: Element) {
		super.readExternal(element)
		taskName = element.getAttributeValue("taskName") ?: ""
		configFilePath = element.getAttributeValue("configFilePath") ?: ""
		workingDirectory = element.getAttributeValue("workingDirectory") ?: ""
		nadleArguments = element.getAttributeValue("nadleArguments") ?: ""
		val refName = element.getAttributeValue("nodeInterpreter")
		interpreterRef = if (refName != null) {
			NodeJsInterpreterRef.create(refName)
		} else {
			NodeJsInterpreterRef.createProjectRef()
		}
		envData = EnvironmentVariablesData.readExternal(element)
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

			val effectiveWorkDir = configuration.workingDirectory
				.ifBlank { projectBasePath }

			val nodePath = NodeJsResolver.resolve(
				environment.project,
				configuration.interpreterRef
			) ?: throw ExecutionException(
				"Node.js not found. Configure it in " +
					"Settings > Languages & Frameworks > Node.js."
			)

			val commandLine = if (isDebug) {
				buildDebugCommandLine(projectBasePath, nodePath)
			} else {
				buildRunCommandLine(projectBasePath, nodePath)
			}

			commandLine.workDirectory = File(effectiveWorkDir)

			val extraArgs = ParametersListUtil.parse(
				configuration.nadleArguments
			)
			commandLine.addParameters(extraArgs)

			configuration.envData.configureCommandLine(commandLine, true)

			return try {
				KillableColoredProcessHandler(commandLine)
			} catch (e: ExecutionException) {
				throw ExecutionException(
					"Failed to start Nadle task: ${e.message}", e
				)
			}
		}

		private fun buildRunCommandLine(
			projectBasePath: String,
			nodePath: String
		): GeneralCommandLine {
			val npxPath = Path.of(nodePath).parent?.resolve("npx")?.toString()
				?: "npx"

			return GeneralCommandLine().apply {
				exePath = npxPath
				addParameter("nadle")
				addParameter(configuration.taskName)
				addParameter("--no-footer")
			}
		}

		private fun buildDebugCommandLine(
			projectBasePath: String,
			nodePath: String
		): GeneralCommandLine {
			val nadleBin = resolveNadleBinary(projectBasePath)

			return GeneralCommandLine().apply {
				exePath = nodePath
				addParameter("--inspect-brk")
				addParameter(nadleBin)
				addParameter(configuration.taskName)
				addParameter("--no-footer")
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
