package com.github.nadlejs.intellij.plugin.toolwindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

class NadleToolWindowFactory : ToolWindowFactory {

	override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
		val panel = NadleToolWindowPanel(project)
		Disposer.register(toolWindow.disposable, panel)
		val content = ContentFactory.getInstance().createContent(panel, "", false)
		toolWindow.contentManager.addContent(content)
	}
}
