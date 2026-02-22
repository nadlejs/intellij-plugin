package com.github.nadlejs.intellij.plugin.search

import com.github.nadlejs.intellij.plugin.run.NadleTask
import com.github.nadlejs.intellij.plugin.run.NadleTaskConfigurationType
import com.github.nadlejs.intellij.plugin.run.NadleTaskRunConfiguration
import com.github.nadlejs.intellij.plugin.run.NadleTaskScanner
import com.github.nadlejs.intellij.plugin.util.NadleIcons
import com.intellij.execution.runners.ExecutionUtil
import com.intellij.execution.RunManager
import com.intellij.execution.configurations.ConfigurationTypeUtil
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.ide.actions.searcheverywhere.FoundItemDescriptor
import com.intellij.ide.actions.searcheverywhere.WeightedSearchEverywhereContributor
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.project.Project
import com.intellij.psi.codeStyle.MinusculeMatcher
import com.intellij.psi.codeStyle.NameUtil
import com.intellij.util.Processor
import java.awt.Component
import java.nio.file.Path
import javax.swing.DefaultListCellRenderer
import javax.swing.JList
import javax.swing.ListCellRenderer

class NadleTaskSearchEverywhereContributor(
	private val project: Project
) : WeightedSearchEverywhereContributor<NadleTask> {

	override fun getSearchProviderId(): String = javaClass.name

	override fun getGroupName(): String = "Nadle Tasks"

	override fun getSortWeight(): Int = 300

	override fun showInFindResults(): Boolean = false

	override fun isEmptyPatternSupported(): Boolean = true

	override fun getElementsRenderer(): ListCellRenderer<in NadleTask> {
		return object : DefaultListCellRenderer() {
			override fun getListCellRendererComponent(
				list: JList<*>?,
				value: Any?,
				index: Int,
				isSelected: Boolean,
				cellHasFocus: Boolean
			): Component {
				val component = super.getListCellRendererComponent(
					list, value, index, isSelected, cellHasFocus
				)
				if (value is NadleTask) {
					text = value.name
					icon = NadleIcons.Nadle
				}
				return component
			}
		}
	}

	override fun getDataForItem(element: NadleTask, dataId: String): Any? = null

	override fun fetchWeightedElements(
		pattern: String,
		progressIndicator: ProgressIndicator,
		consumer: Processor<in FoundItemDescriptor<NadleTask>>
	) {
		val basePath = project.basePath ?: return
		val tasks = NadleTaskScanner.scanTasksDetailed(Path.of(basePath))

		val matcher = if (pattern.isNotEmpty()) {
			NameUtil.buildMatcher("*$pattern").build()
		} else {
			null
		}

		for (task in tasks) {
			progressIndicator.checkCanceled()
			if (matcher == null || matcher.matches(task.name)) {
				val weight = matcher?.matchingDegree(task.name) ?: 0
				consumer.process(FoundItemDescriptor(task, weight))
			}
		}
	}

	override fun processSelectedItem(
		selected: NadleTask,
		modifiers: Int,
		searchText: String
	): Boolean {
		val runManager = RunManager.getInstance(project)
		val configType = ConfigurationTypeUtil.findConfigurationType(
			NadleTaskConfigurationType::class.java
		)
		val factory = configType.configurationFactories[0]

		val existing = runManager.allSettings.find { settings ->
			val config = settings.configuration
			config is NadleTaskRunConfiguration
				&& config.taskName == selected.name
				&& config.configFilePath == selected.configFilePath.toString()
		}

		val settings = existing ?: runManager.createConfiguration(
			selected.name,
			factory
		).also { settings ->
			val config = settings.configuration as NadleTaskRunConfiguration
			config.taskName = selected.name
			config.configFilePath = selected.configFilePath.toString()
			config.workingDirectory = selected.workingDirectory.toString()
			settings.isTemporary = true
			runManager.addConfiguration(settings)
		}

		runManager.selectedConfiguration = settings
		ExecutionUtil.runConfiguration(settings, DefaultRunExecutor.getRunExecutorInstance())
		return true
	}

	override fun fetchElements(
		pattern: String,
		progressIndicator: ProgressIndicator,
		consumer: Processor<in NadleTask>
	) {
		fetchWeightedElements(pattern, progressIndicator) { descriptor ->
			consumer.process(descriptor.item)
		}
	}
}
