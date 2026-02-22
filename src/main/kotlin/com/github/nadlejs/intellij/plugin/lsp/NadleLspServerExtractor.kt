package com.github.nadlejs.intellij.plugin.lsp

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.diagnostic.Logger
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

object NadleLspServerExtractor {

	private val LOG = Logger.getInstance(NadleLspServerExtractor::class.java)

	private val SERVER_FILES = listOf(
		"server.mjs",
		"package.json",
		"lib/server.js"
	)

	private const val RESOURCE_PREFIX = "/language-server/"

	fun getServerPath(): Path? {
		val extractDir = getExtractDirectory()
		val serverMjs = extractDir.resolve("server.mjs")

		if (Files.exists(serverMjs)) {
			return serverMjs
		}

		return try {
			extractServer(extractDir)
			serverMjs
		} catch (e: Exception) {
			LOG.warn("Failed to extract bundled language server", e)
			null
		}
	}

	private fun extractServer(extractDir: Path) {
		Files.createDirectories(extractDir.resolve("lib"))

		for (file in SERVER_FILES) {
			extractResource(RESOURCE_PREFIX + file, extractDir.resolve(file))
		}

		// Extract chunk files (name contains hash)
		extractChunks(extractDir.resolve("lib"))
	}

	private fun extractChunks(libDir: Path) {
		val chunkStream = javaClass.getResourceAsStream("/language-server/lib/") ?: return

		// Since we can't list JAR resources directly, extract known chunk
		// by scanning the lib directory resource listing
		val libResource = javaClass.getResource("/language-server/lib/")
		if (libResource != null) {
			// Try to find chunk files from classloader
			val classLoader = javaClass.classLoader
			val resourceUrl = classLoader.getResource("language-server/lib/")
			if (resourceUrl != null) {
				// Read the directory listing if available
				try {
					val connection = resourceUrl.openConnection()
					if (connection is java.net.JarURLConnection) {
						val jar = connection.jarFile
						val entries = jar.entries()
						while (entries.hasMoreElements()) {
							val entry = entries.nextElement()
							if (entry.name.startsWith("language-server/lib/chunk-")
								&& entry.name.endsWith(".js")
							) {
								val fileName = entry.name.substringAfterLast("/")
								val target = libDir.resolve(fileName)
								jar.getInputStream(entry).use { input ->
									Files.copy(
										input, target,
										StandardCopyOption.REPLACE_EXISTING
									)
								}
							}
						}
					}
				} catch (e: Exception) {
					LOG.warn("Failed to extract chunk files via JAR listing", e)
				}
			}
		}

		// Fallback: try known chunk pattern
		if (Files.list(libDir).noneMatch {
				it.fileName.toString().startsWith("chunk-")
			}) {
			// Try extracting with the known filename from build time
			val knownChunks = listOf("chunk-KPQ3WXPR.js")
			for (chunk in knownChunks) {
				extractResource(
					RESOURCE_PREFIX + "lib/" + chunk,
					libDir.resolve(chunk)
				)
			}
		}
	}

	private fun extractResource(resourcePath: String, target: Path) {
		val stream: InputStream = javaClass.getResourceAsStream(resourcePath)
			?: throw IllegalStateException("Resource not found: $resourcePath")

		stream.use {
			Files.copy(it, target, StandardCopyOption.REPLACE_EXISTING)
		}
	}

	private fun getExtractDirectory(): Path {
		return Path.of(
			PathManager.getPluginsPath(),
			"nadle-language-server"
		)
	}
}
