package com.github.nadlejs.intellij.plugin.lsp

import com.github.nadlejs.intellij.plugin.util.NadleFileUtil
import com.github.nadlejs.intellij.plugin.util.NodeJsResolver
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import java.nio.file.Files

class NadleLspServerDescriptor(
	project: Project
) : ProjectWideLspServerDescriptor(project, "Nadle Language Server") {

	override fun isSupportedFile(file: VirtualFile): Boolean =
		NadleFileUtil.isNadleConfigFile(file)

	override fun createCommandLine(): GeneralCommandLine {
		val nodePath = NodeJsResolver.resolve(project)
			?: throw IllegalStateException(
				"Node.js is required for Nadle language intelligence features. " +
					"Please configure Node.js in Settings > Languages & Frameworks > Node.js, " +
					"or ensure it is available in PATH."
			)

		val serverPath = resolveServerPath()
			?: throw IllegalStateException(
				"Nadle language server not found. " +
					"Please reinstall the plugin."
			)

		return GeneralCommandLine(nodePath, serverPath)
	}

	private fun resolveServerPath(): String? {
		val bundled = NadleLspServerExtractor.getServerPath()
		if (bundled != null && Files.exists(bundled)) {
			LOG.info("Using bundled Nadle language server at: $bundled")
			return bundled.toString()
		}

		return null
	}

	companion object {
		private val LOG = Logger.getInstance(NadleLspServerDescriptor::class.java)
	}
}
