package com.github.nadlejs.intellij.plugin

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider

class NadleLspServerSupportProvider : LspServerSupportProvider {

	override fun fileOpened(
		project: Project,
		file: VirtualFile,
		serverStarter: LspServerSupportProvider.LspServerStarter
	) {
		if (!NadleFileUtil.isNadleConfigFile(file)) {
			return
		}

		try {
			serverStarter.ensureServerStarted(
				NadleLspServerDescriptor(project)
			)
		} catch (e: Exception) {
			LOG.warn(
				"Failed to start Nadle language server. " +
					"Language intelligence features will be unavailable.",
				e
			)
		}
	}

	companion object {
		private val LOG = Logger.getInstance(
			NadleLspServerSupportProvider::class.java
		)
	}
}
