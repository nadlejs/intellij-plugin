package com.github.nadlejs.intellij.plugin.service

import com.github.nadlejs.intellij.plugin.util.NadleFileUtil
import com.github.nadlejs.intellij.plugin.util.NodeJsResolver
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicReference

@Service(Service.Level.PROJECT)
class NadleProjectService(private val project: Project) : Disposable {

	data class ProjectInfo(
		val packageManager: String,
		val projectDir: String,
		val rootWorkspace: WorkspaceInfo,
		val workspaces: List<WorkspaceInfo>,
		val allConfigFiles: List<String>
	)

	data class WorkspaceInfo(
		val id: String,
		val absolutePath: String,
		val relativePath: String,
		val dependencies: List<String>,
		val configFilePath: String?
	)

	private val cachedProject = AtomicReference<ProjectInfo?>()
	private val gson = Gson()

	init {
		subscribeToVfsChanges()
	}

	fun getProjectInfo(): ProjectInfo? {
		cachedProject.get()?.let { return it }
		return refresh()
	}

	fun refresh(): ProjectInfo? {
		val basePath = project.basePath ?: return null
		val result = runCli(basePath)
		cachedProject.set(result)
		return result
	}

	override fun dispose() {
		cachedProject.set(null)
	}

	private fun resolveCliPath(): String? {
		val basePath = project.basePath ?: return null
		val nodeModules = Path.of(basePath, "node_modules")

		val direct = nodeModules.resolve("@nadle/project-resolver/cli.mjs")
		if (Files.exists(direct)) return direct.toString()

		val nadleLink = nodeModules.resolve("nadle")
		if (Files.exists(nadleLink)) {
			val sibling = nadleLink.toRealPath().parent
				.resolve("@nadle/project-resolver/cli.mjs")
			if (Files.exists(sibling)) return sibling.toString()
		}

		return null
	}

	private fun runCli(cwd: String): ProjectInfo? {
		val nodePath = NodeJsResolver.resolve(project) ?: run {
			LOG.info("Node.js not found, project-resolver unavailable")
			return null
		}

		val cliPath = resolveCliPath() ?: run {
			LOG.info("nadle-project-resolver CLI not found")
			return null
		}

		return try {
			val process = ProcessBuilder(nodePath, cliPath, "--cwd", cwd)
				.directory(java.io.File(cwd))
				.redirectErrorStream(false)
				.start()

			val stdout = process.inputStream.bufferedReader().readText()
			val stderr = process.errorStream.bufferedReader().readText()
			val exitCode = process.waitFor()

			if (exitCode != 0) {
				LOG.info("nadle-project-resolver exited with code $exitCode: $stderr")
				return null
			}

			parseOutput(stdout)
		} catch (e: Exception) {
			LOG.warn("Failed to run nadle-project-resolver", e)
			null
		}
	}

	private fun parseOutput(json: String): ProjectInfo? {
		return try {
			gson.fromJson(json, CliOutput::class.java)?.toProjectInfo()
		} catch (e: Exception) {
			LOG.warn("Failed to parse nadle-project-resolver output", e)
			null
		}
	}

	private fun subscribeToVfsChanges() {
		project.messageBus.connect(this).subscribe(
			VirtualFileManager.VFS_CHANGES,
			object : BulkFileListener {
				override fun after(events: List<VFileEvent>) {
					val hasRelevantChange = events.any { event ->
						when (event) {
							is VFileContentChangeEvent -> isStructureFile(event.file.name)
							is VFileCreateEvent -> isStructureFile(event.childName)
							is VFileDeleteEvent -> isStructureFile(event.file.name)
							else -> false
						}
					}
					if (hasRelevantChange) {
						cachedProject.set(null)
					}
				}
			}
		)
	}

	private fun isStructureFile(name: String): Boolean =
		name == "pnpm-workspace.yaml" ||
			name == "package.json" ||
			NadleFileUtil.isNadleConfigFileName(name)

	private data class CliOutput(
		val packageManager: String?,
		val projectDir: String?,
		val rootWorkspace: CliWorkspace?,
		val workspaces: List<CliWorkspace>?,
		val allConfigFiles: List<String>?
	) {
		fun toProjectInfo(): ProjectInfo? {
			val root = rootWorkspace?.toWorkspaceInfo() ?: return null
			return ProjectInfo(
				packageManager = packageManager ?: "unknown",
				projectDir = projectDir ?: "",
				rootWorkspace = root,
				workspaces = workspaces?.map { it.toWorkspaceInfo() } ?: emptyList(),
				allConfigFiles = allConfigFiles ?: emptyList()
			)
		}
	}

	private data class CliWorkspace(
		val id: String?,
		val absolutePath: String?,
		val relativePath: String?,
		val dependencies: List<String>?,
		@SerializedName("configFilePath") val configFilePath: String?
	) {
		fun toWorkspaceInfo(): WorkspaceInfo = WorkspaceInfo(
			id = id ?: "",
			absolutePath = absolutePath ?: "",
			relativePath = relativePath ?: "",
			dependencies = dependencies ?: emptyList(),
			configFilePath = configFilePath
		)
	}

	companion object {
		private val LOG = Logger.getInstance(NadleProjectService::class.java)

		fun getInstance(project: Project): NadleProjectService =
			project.getService(NadleProjectService::class.java)
	}
}
