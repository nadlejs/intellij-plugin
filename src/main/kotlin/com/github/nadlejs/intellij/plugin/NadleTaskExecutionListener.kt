package com.github.nadlejs.intellij.plugin

import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.psi.PsiManager

class NadleTaskExecutionListener : ProjectActivity {

	override suspend fun execute(project: Project) {
		subscribeToExecution(project)
		subscribeToFileChanges(project)
	}

	private fun subscribeToExecution(project: Project) {
		project.messageBus.connect().subscribe(
			ExecutionManager.EXECUTION_TOPIC,
			object : ExecutionListener {
				override fun processTerminated(
					executorId: String,
					env: ExecutionEnvironment,
					handler: ProcessHandler,
					exitCode: Int
				) {
					val configuration = env.runnerAndConfigurationSettings
						?.configuration as? NadleTaskRunConfiguration
						?: return

					val taskName = configuration.taskName
					val filePath = configuration.configFilePath
					if (taskName.isBlank() || filePath.isBlank()) return

					val result = if (exitCode == 0) {
						NadleTaskStateService.TaskResult.PASSED
					} else {
						NadleTaskStateService.TaskResult.FAILED
					}

					NadleTaskStateService.getInstance(project)
						.setResult(filePath, taskName, result)

					// Trigger editor reparse so gutter icons update
					PsiManager.getInstance(project).dropPsiCaches()
				}
			}
		)
	}

	private fun subscribeToFileChanges(project: Project) {
		project.messageBus.connect().subscribe(
			VirtualFileManager.VFS_CHANGES,
			object : BulkFileListener {
				override fun after(events: List<VFileEvent>) {
					val project = events.firstOrNull()
						?.file?.let { return@let project } ?: return

					for (event in events) {
						if (event !is VFileContentChangeEvent) continue
						val file = event.file
						if (!NadleFileUtil.isNadleConfigFile(file)) continue

						NadleTaskStateService.getInstance(project)
							.clearResultsForFile(file.path)
					}
				}
			}
		)
	}
}
