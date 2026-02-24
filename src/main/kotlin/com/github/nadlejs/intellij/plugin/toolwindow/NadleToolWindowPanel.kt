package com.github.nadlejs.intellij.plugin.toolwindow

import com.github.nadlejs.intellij.plugin.run.NadleTask
import com.github.nadlejs.intellij.plugin.run.NadleTaskRunner
import com.github.nadlejs.intellij.plugin.run.NadleTaskScanner
import com.github.nadlejs.intellij.plugin.run.NadleTaskStateService
import com.github.nadlejs.intellij.plugin.service.NadleProjectService
import com.github.nadlejs.intellij.plugin.util.NadleFileUtil
import com.github.nadlejs.intellij.plugin.util.NadleIcons
import com.github.nadlejs.intellij.plugin.util.NadleKernel
import com.intellij.execution.ExecutionListener
import com.intellij.execution.ExecutionManager
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Separator
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.PopupHandler
import com.intellij.ui.ScrollPaneFactory
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.treeStructure.Tree
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.file.Path
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath

class NadleToolWindowPanel(
	private val project: Project
) : com.intellij.openapi.ui.SimpleToolWindowPanel(true, true), Disposable {

	private val rootNode = DefaultMutableTreeNode()
	private val treeModel = DefaultTreeModel(rootNode)
	private val tree = Tree(treeModel)

	init {
		tree.isRootVisible = false
		tree.showsRootHandles = true
		tree.cellRenderer = NadleTreeCellRenderer()
		tree.emptyText.setText("No nadle configuration files found")

		setupSpeedSearch()
		setupDoubleClickHandler()
		setupEnterKeyHandler()
		setupContextMenu()
		setupToolbar()

		setContent(ScrollPaneFactory.createScrollPane(tree))
		subscribeToFileChanges()
		subscribeToExecutionResults()
		loadTasks()
	}

	fun loadTasks() {
		ApplicationManager.getApplication().executeOnPooledThread {
			val basePath = project.basePath ?: return@executeOnPooledThread
			val tasks = loadTasksFromProjectService()
				?: NadleTaskScanner.scanTasksDetailed(Path.of(basePath))

			ApplicationManager.getApplication().invokeLater {
				if (project.isDisposed) return@invokeLater
				rebuildTree(tasks)
			}
		}
	}

	private fun loadTasksFromProjectService(): List<NadleTask>? {
		val projectInfo = NadleProjectService.getInstance(project).getProjectInfo()
			?: return null

		val tasks = mutableListOf<NadleTask>()
		val allWorkspaces = listOf(projectInfo.rootWorkspace) + projectInfo.workspaces
		val projectDir = Path.of(projectInfo.projectDir)

		for (workspace in allWorkspaces) {
			val configPath = workspace.configFilePath ?: continue
			val configFile = Path.of(configPath)
			if (!java.nio.file.Files.exists(configFile)) continue

			val text = try {
				java.nio.file.Files.readString(configFile)
			} catch (_: java.io.IOException) {
				continue
			}

			val taskNames = NadleFileUtil.extractAllTaskNames(text)
			val isRoot = NadleKernel.isRootWorkspaceId(workspace.id)

			for (name in taskNames) {
				val qualifiedName = NadleKernel.composeTaskIdentifier(
					if (isRoot) "" else workspace.id,
					name
				)
				tasks.add(
					NadleTask(
						name = qualifiedName,
						configFilePath = configFile.toAbsolutePath(),
						workingDirectory = projectDir.toAbsolutePath()
					)
				)
			}
		}

		return tasks
	}

	override fun dispose() {}

	private fun setupSpeedSearch() {
		TreeSpeedSearch.installOn(tree, false) { path: TreePath ->
			val node = path.lastPathComponent as? DefaultMutableTreeNode
				?: return@installOn ""
			when (val userObject = node.userObject) {
				is NadleTask -> NadleKernel.parseTaskReference(userObject.name).taskName
				is String -> userObject
				else -> ""
			}
		}
	}

	private fun rebuildTree(tasks: List<NadleTask>) {
		rootNode.removeAllChildren()

		val grouped = tasks.groupBy { it.configFilePath }
		val basePath = project.basePath ?: ""

		for ((configPath, fileTasks) in grouped.entries.sortedBy { it.key.toString() }) {
			val relativeDir = Path.of(basePath).relativize(configPath.parent).toString()
			val workspaceId = NadleKernel.deriveWorkspaceId(relativeDir.ifEmpty { "." })
			val isRoot = NadleKernel.isRootWorkspaceId(workspaceId)

			if (isRoot) {
				for (task in fileTasks.sortedBy { it.name }) {
					rootNode.add(DefaultMutableTreeNode(task))
				}
			} else {
				val label = workspaceId
				val configNode = DefaultMutableTreeNode(label)
				for (task in fileTasks.sortedBy { it.name }) {
					configNode.add(DefaultMutableTreeNode(task))
				}
				rootNode.add(configNode)
			}
		}

		treeModel.reload()

		for (i in 0 until tree.rowCount) {
			tree.expandRow(i)
		}
	}

	private fun getSelectedTask(): NadleTask? {
		val node = tree.lastSelectedPathComponent as? DefaultMutableTreeNode
			?: return null
		return node.userObject as? NadleTask
	}

	private fun setupDoubleClickHandler() {
		tree.addMouseListener(object : MouseAdapter() {
			override fun mouseClicked(e: MouseEvent) {
				if (e.clickCount == 2) {
					val task = getSelectedTask() ?: return
					NadleTaskRunner.run(project, task)
				}
			}
		})
	}

	private fun setupEnterKeyHandler() {
		tree.addKeyListener(object : KeyAdapter() {
			override fun keyPressed(e: KeyEvent) {
				if (e.keyCode == KeyEvent.VK_ENTER) {
					val task = getSelectedTask() ?: return
					NadleTaskRunner.run(project, task)
				}
			}
		})
	}

	private fun setupContextMenu() {
		val popupGroup = DefaultActionGroup(
			RunAction(),
			DebugAction(),
			Separator.getInstance(),
			CreateRunConfigAction()
		)
		PopupHandler.installFollowingSelectionTreePopup(
			tree,
			popupGroup,
			"NadleToolWindowPopup"
		)
	}

	private fun setupToolbar() {
		val actionGroup = DefaultActionGroup(
			RunAction(),
			DebugAction(),
			Separator.getInstance(),
			RefreshAction(),
			Separator.getInstance(),
			ExpandAllAction(),
			CollapseAllAction()
		)

		val toolbar = ActionManager.getInstance()
			.createActionToolbar("NadleToolWindow", actionGroup, true)
		toolbar.targetComponent = this
		setToolbar(toolbar.component)
	}

	private fun subscribeToFileChanges() {
		project.messageBus.connect(this).subscribe(
			VirtualFileManager.VFS_CHANGES,
			object : BulkFileListener {
				override fun after(events: List<VFileEvent>) {
					val hasNadleChange = events.any { event ->
						when (event) {
							is VFileContentChangeEvent ->
								NadleFileUtil.isNadleConfigFile(event.file)
							is VFileCreateEvent ->
								NadleFileUtil.isNadleConfigFileName(event.childName)
							is VFileDeleteEvent ->
								NadleFileUtil.isNadleConfigFileName(event.file.name)
							else -> false
						}
					}
					if (hasNadleChange) {
						loadTasks()
					}
				}
			}
		)
	}

	private fun subscribeToExecutionResults() {
		project.messageBus.connect(this).subscribe(
			ExecutionManager.EXECUTION_TOPIC,
			object : ExecutionListener {
				override fun processTerminated(
					executorId: String,
					env: ExecutionEnvironment,
					handler: ProcessHandler,
					exitCode: Int
				) {
					val configuration = env.runnerAndConfigurationSettings
						?.configuration as? com.github.nadlejs.intellij.plugin.run.NadleTaskRunConfiguration
						?: return

					if (configuration.taskName.isNotBlank()) {
						ApplicationManager.getApplication().invokeLater {
							if (!project.isDisposed) {
								tree.repaint()
							}
						}
					}
				}
			}
		)
	}

	private inner class NadleTreeCellRenderer : ColoredTreeCellRenderer() {
		override fun customizeCellRenderer(
			tree: JTree,
			value: Any?,
			selected: Boolean,
			expanded: Boolean,
			leaf: Boolean,
			row: Int,
			hasFocus: Boolean
		) {
			val node = value as? DefaultMutableTreeNode ?: return
			when (val userObject = node.userObject) {
				is String -> {
					icon = AllIcons.Nodes.Folder
					append(userObject, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
				}
				is NadleTask -> {
					val stateService = NadleTaskStateService.getInstance(project)
					val result = stateService.getResult(
						userObject.configFilePath.toString(),
						userObject.name
					)
					icon = when (result) {
						NadleTaskStateService.TaskResult.PASSED ->
							AllIcons.RunConfigurations.TestPassed
						NadleTaskStateService.TaskResult.FAILED ->
							AllIcons.RunConfigurations.TestFailed
						null -> NadleIcons.Nadle
					}
					val displayName = NadleKernel.parseTaskReference(userObject.name).taskName
					append(displayName)
				}
			}
		}
	}

	private inner class RunAction : AnAction(
		"Run",
		"Run selected task",
		AllIcons.Actions.Execute
	) {
		override fun actionPerformed(e: AnActionEvent) {
			val task = getSelectedTask() ?: return
			NadleTaskRunner.run(project, task)
		}

		override fun update(e: AnActionEvent) {
			e.presentation.isEnabled = getSelectedTask() != null
		}

		override fun getActionUpdateThread() = ActionUpdateThread.EDT
	}

	private inner class DebugAction : AnAction(
		"Debug",
		"Debug selected task",
		AllIcons.Actions.StartDebugger
	) {
		override fun actionPerformed(e: AnActionEvent) {
			val task = getSelectedTask() ?: return
			NadleTaskRunner.debug(project, task)
		}

		override fun update(e: AnActionEvent) {
			e.presentation.isEnabled = getSelectedTask() != null
		}

		override fun getActionUpdateThread() = ActionUpdateThread.EDT
	}

	private inner class RefreshAction : AnAction(
		"Refresh",
		"Refresh task list",
		AllIcons.Actions.Refresh
	) {
		override fun actionPerformed(e: AnActionEvent) {
			loadTasks()
		}

		override fun getActionUpdateThread() = ActionUpdateThread.BGT
	}

	private inner class CreateRunConfigAction : AnAction(
		"Create Run Configuration",
		"Create a persistent run configuration for this task",
		AllIcons.General.Add
	) {
		override fun actionPerformed(e: AnActionEvent) {
			val task = getSelectedTask() ?: return
			NadleTaskRunner.createRunConfiguration(project, task)
		}

		override fun update(e: AnActionEvent) {
			e.presentation.isEnabled = getSelectedTask() != null
		}

		override fun getActionUpdateThread() = ActionUpdateThread.EDT
	}

	private inner class ExpandAllAction : AnAction(
		"Expand All",
		"Expand all tree nodes",
		AllIcons.Actions.Expandall
	) {
		override fun actionPerformed(e: AnActionEvent) {
			for (i in 0 until tree.rowCount) {
				tree.expandRow(i)
			}
		}

		override fun getActionUpdateThread() = ActionUpdateThread.EDT
	}

	private inner class CollapseAllAction : AnAction(
		"Collapse All",
		"Collapse all tree nodes",
		AllIcons.Actions.Collapseall
	) {
		override fun actionPerformed(e: AnActionEvent) {
			for (i in tree.rowCount - 1 downTo 0) {
				tree.collapseRow(i)
			}
		}

		override fun getActionUpdateThread() = ActionUpdateThread.EDT
	}

}
