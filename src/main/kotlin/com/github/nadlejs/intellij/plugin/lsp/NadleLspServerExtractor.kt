package com.github.nadlejs.intellij.plugin.lsp

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object NadleLspServerExtractor {

	private val LOG = Logger.getInstance(NadleLspServerExtractor::class.java)

	private const val RESOURCE_PREFIX = "language-server/"
	private const val MANIFEST = "manifest.txt"
	private const val VERSION_FILE = ".version"

	fun getServerPath(): Path? {
		val extractDir = getExtractDirectory()
		val serverMjs = extractDir.resolve("server.mjs")

		val currentVersion = getResourceVersion()
		val extractedVersion = readFile(extractDir.resolve(VERSION_FILE))

		if (Files.exists(serverMjs) && currentVersion == extractedVersion) {
			return serverMjs
		}

		return try {
			if (Files.exists(extractDir)) {
				extractDir.toFile().deleteRecursively()
			}
			extractServer(extractDir)
			Files.writeString(extractDir.resolve(VERSION_FILE), currentVersion)
			if (Files.exists(serverMjs)) serverMjs else null
		} catch (e: Exception) {
			LOG.warn("Failed to extract bundled language server", e)
			null
		}
	}

	private fun extractServer(extractDir: Path) {
		Files.createDirectories(extractDir.resolve("lib"))

		val manifest = loadManifest() ?: run {
			LOG.warn("Language server manifest not found in resources")
			return
		}

		for (file in manifest) {
			val stream = javaClass.classLoader
				.getResourceAsStream("$RESOURCE_PREFIX$file") ?: run {
				LOG.debug("Skipping missing resource: $file")
				continue
			}

			val target = extractDir.resolve(file)
			Files.createDirectories(target.parent)
			stream.use { Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING) }
		}
	}

	private fun loadManifest(): List<String>? {
		val stream = javaClass.classLoader
			.getResourceAsStream("$RESOURCE_PREFIX$MANIFEST") ?: return null
		return stream.bufferedReader().use { it.readLines() }
			.filter { it.isNotBlank() }
	}

	private fun getResourceVersion(): String {
		val manifest = loadManifest() ?: return "unknown"
		return manifest.sorted().joinToString(",").hashCode().toString()
	}

	private fun readFile(path: Path): String? =
		if (Files.exists(path)) Files.readString(path).trim() else null

	private fun getExtractDirectory(): Path =
		Path.of(PathManager.getPluginsPath(), "nadle-language-server")
}
