package com.github.nadlejs.intellij.plugin

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import java.nio.file.Files
import java.nio.file.Path

class NadleLspServerDescriptor(
	project: Project
) : ProjectWideLspServerDescriptor(project, "Nadle Language Server") {

	override fun isSupportedFile(file: VirtualFile): Boolean =
		NadleFileUtil.isNadleConfigFile(file)

	override fun createCommandLine(): GeneralCommandLine {
		if (!isNodeAvailable()) {
			throw IllegalStateException(
				"Node.js is required for Nadle language intelligence features. " +
					"Please install Node.js and ensure it is available in PATH."
			)
		}

		val serverPath = resolveServerPath()
			?: throw IllegalStateException(
				"Nadle language server not found. " +
					"Install @nadle/language-server as a project dependency " +
					"or globally."
			)

		return GeneralCommandLine("node", serverPath)
	}

	private fun isNodeAvailable(): Boolean {
		return try {
			val process = ProcessBuilder("node", "--version")
				.redirectErrorStream(true)
				.start()
			val exitCode = process.waitFor()
			exitCode == 0
		} catch (e: Exception) {
			LOG.warn("Node.js not found in PATH", e)
			false
		}
	}

	private fun resolveServerPath(): String? {
		val basePath = project.basePath ?: return null

		// Check project-local node_modules first
		val localServer = Path.of(
			basePath,
			"node_modules",
			"@nadle",
			"language-server",
			"server.mjs"
		)
		if (Files.exists(localServer)) {
			return localServer.toString()
		}

		// Try global resolution via which/where
		return try {
			val process = ProcessBuilder("which", "nadle-language-server")
				.redirectErrorStream(true)
				.start()
			val output = process.inputStream.bufferedReader().readText().trim()
			val exitCode = process.waitFor()
			if (exitCode == 0 && output.isNotBlank()) output else null
		} catch (e: Exception) {
			LOG.warn("Could not resolve global nadle-language-server", e)
			null
		}
	}

	companion object {
		private val LOG = Logger.getInstance(NadleLspServerDescriptor::class.java)
	}
}
