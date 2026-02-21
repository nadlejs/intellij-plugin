package com.github.nadlejs.intellij.plugin.util

import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterManager
import com.intellij.javascript.nodejs.interpreter.NodeJsInterpreterRef
import com.intellij.javascript.nodejs.interpreter.local.NodeJsLocalInterpreter
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.util.EnvironmentUtil
import java.nio.file.Files
import java.nio.file.Path

/**
 * Resolves the Node.js binary path in a way that works from GUI applications
 * on macOS, where the shell PATH is not inherited.
 */
object NodeJsResolver {

	private val LOG = Logger.getInstance(NodeJsResolver::class.java)

	/**
	 * Resolves the full path to the `node` binary, or null if not found.
	 *
	 * Resolution order:
	 * 1. Explicit interpreter ref (if provided)
	 * 2. IntelliJ's configured Node.js interpreter (Settings > Languages > Node.js)
	 * 3. Shell environment PATH via EnvironmentUtil
	 * 4. Well-known installation paths (Homebrew, nvm, fnm, Volta, etc.)
	 */
	fun resolve(
		project: Project,
		interpreterRef: NodeJsInterpreterRef? = null
	): String? {
		return resolveFromRef(project, interpreterRef)
			?: resolveFromInterpreterManager(project)
			?: resolveFromShellPath()
			?: resolveFromWellKnownPaths()
	}

	private fun resolveFromRef(
		project: Project,
		ref: NodeJsInterpreterRef?
	): String? {
		if (ref == null) return null
		return try {
			val interpreter = ref.resolve(project)
			if (interpreter is NodeJsLocalInterpreter) {
				val path = interpreter.interpreterSystemDependentPath
				LOG.info("Resolved Node.js from explicit interpreter ref: $path")
				path
			} else {
				null
			}
		} catch (e: Exception) {
			LOG.debug("Could not resolve Node.js from interpreter ref", e)
			null
		}
	}

	private fun resolveFromInterpreterManager(project: Project): String? {
		return try {
			val interpreter = NodeJsInterpreterManager.getInstance(project).interpreter
			if (interpreter is NodeJsLocalInterpreter) {
				val path = interpreter.interpreterSystemDependentPath
				LOG.info("Resolved Node.js from IntelliJ interpreter settings: $path")
				path
			} else {
				null
			}
		} catch (e: Exception) {
			LOG.debug("Could not resolve Node.js from interpreter manager", e)
			null
		}
	}

	private fun resolveFromShellPath(): String? {
		return try {
			val shellPath = EnvironmentUtil.getEnvironmentMap()["PATH"] ?: return null
			val nodePath = shellPath.split(":")
				.asSequence()
				.map { Path.of(it, "node") }
				.firstOrNull { Files.isExecutable(it) }

			if (nodePath != null) {
				LOG.info("Resolved Node.js from shell PATH: $nodePath")
			}
			nodePath?.toString()
		} catch (e: Exception) {
			LOG.debug("Could not resolve Node.js from shell PATH", e)
			null
		}
	}

	private fun resolveFromWellKnownPaths(): String? {
		val home = System.getProperty("user.home") ?: return null

		val candidates = listOf(
			"/opt/homebrew/bin/node",
			"/usr/local/bin/node",
			"$home/.nvm/current/bin/node",
			"$home/.local/share/fnm/aliases/default/bin/node",
			"$home/.volta/bin/node",
			"$home/.asdf/shims/node",
			"$home/.local/bin/node",
			"/usr/bin/node",
		)

		val found = candidates.firstOrNull { Files.isExecutable(Path.of(it)) }
		if (found != null) {
			LOG.info("Resolved Node.js from well-known path: $found")
		}
		return found
	}
}
