package com.github.nadlejs.intellij.plugin

import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.LabeledComponent
import javax.swing.BoxLayout
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

class NadleTaskSettingsEditor : SettingsEditor<NadleTaskRunConfiguration>() {

	private lateinit var panel: JPanel
	private lateinit var taskNameField: LabeledComponent<JTextField>

	override fun resetEditorFrom(configuration: NadleTaskRunConfiguration) {
		taskNameField.component.text = configuration.taskName
	}

	@Throws(ConfigurationException::class)
	override fun applyEditorTo(configuration: NadleTaskRunConfiguration) {
		configuration.taskName = taskNameField.component.text
	}

	override fun createEditor(): JComponent {
		panel = JPanel().apply {
			layout = BoxLayout(this, BoxLayout.Y_AXIS)
		}

		taskNameField = LabeledComponent<JTextField>().apply {
			component = JTextField()
			text = "Task Name:"
			labelLocation = "West"
		}

		panel.add(taskNameField)

		return panel
	}
}