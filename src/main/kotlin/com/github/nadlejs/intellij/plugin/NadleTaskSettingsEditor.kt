package com.github.nadlejs.intellij.plugin

import com.intellij.codeInsight.AutoPopupController
import com.intellij.execution.configuration.EnvironmentVariablesTextFieldWithBrowseButton
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterField
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.TextFieldWithAutoCompletion
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.panel
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent
import java.nio.file.Path
import javax.swing.JComponent
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.Timer
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class NadleTaskSettingsEditor(
	private val project: Project
) : SettingsEditor<NadleTaskRunConfiguration>() {

	private val completionProvider = NadleTaskCompletionProvider()
	private val interpreterField = NodeJsInterpreterField(project, false)
	private val taskNameField = TextFieldWithAutoCompletion(
		project, completionProvider, false, ""
	)
	private val workingDirectoryField = TextFieldWithBrowseButton().apply {
		addBrowseFolderListener(
			"Working Directory",
			"Select working directory for Nadle task",
			project,
			FileChooserDescriptorFactory.createSingleFolderDescriptor()
		)
	}
	private val nadleArgumentsField = JTextField()
	private val envVarsField = EnvironmentVariablesTextFieldWithBrowseButton()

	private val debounceTimer = Timer(300) { refreshTasks() }.apply {
		isRepeats = false
	}

	init {
		taskNameField.addSettingsProvider { editor ->
			editor.contentComponent.addFocusListener(object : FocusAdapter() {
				override fun focusGained(e: FocusEvent?) {
					SwingUtilities.invokeLater {
						AutoPopupController.getInstance(project)
							.scheduleAutoPopup(editor)
					}
				}
			})
		}

		workingDirectoryField.textField.document.addDocumentListener(
			object : DocumentListener {
				override fun insertUpdate(e: DocumentEvent?) = scheduleRefresh()
				override fun removeUpdate(e: DocumentEvent?) = scheduleRefresh()
				override fun changedUpdate(e: DocumentEvent?) = scheduleRefresh()
			}
		)
	}

	private fun scheduleRefresh() {
		debounceTimer.restart()
	}

	private fun refreshTasks() {
		val workDir = workingDirectoryField.text.takeIf { it.isNotBlank() }
			?: project.basePath
			?: return

		val tasks = NadleTaskScanner.scanTasks(Path.of(workDir))
		completionProvider.updateItems(tasks)
	}

	override fun resetEditorFrom(configuration: NadleTaskRunConfiguration) {
		interpreterField.interpreterRef = configuration.interpreterRef
		taskNameField.text = configuration.taskName
		workingDirectoryField.text = configuration.workingDirectory
		nadleArgumentsField.text = configuration.nadleArguments
		envVarsField.data = configuration.envData
		refreshTasks()
	}

	override fun applyEditorTo(configuration: NadleTaskRunConfiguration) {
		configuration.interpreterRef = interpreterField.interpreterRef
		configuration.taskName = taskNameField.text
		configuration.workingDirectory = workingDirectoryField.text
		configuration.nadleArguments = nadleArgumentsField.text
		configuration.envData = envVarsField.data
	}

	override fun createEditor(): JComponent = panel {
		row("Node &interpreter:") {
			cell(interpreterField).align(AlignX.FILL)
		}
		row("Task &name:") {
			cell(taskNameField).align(AlignX.FILL)
		}
		row("&Working directory:") {
			cell(workingDirectoryField).align(AlignX.FILL)
		}
		row("Nadle &arguments:") {
			cell(nadleArgumentsField).align(AlignX.FILL)
		}
		row("&Environment variables:") {
			cell(envVarsField).align(AlignX.FILL)
		}
	}
}
