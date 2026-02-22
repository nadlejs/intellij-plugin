package com.github.nadlejs.intellij.plugin.run

import com.github.nadlejs.intellij.plugin.util.NadleFileUtil
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes

object NadleTaskScanner {

	private val SKIP_DIRS = setOf(
		"node_modules", ".git", "dist", "build",
		".next", ".nuxt", "coverage", ".turbo", ".cache", "out"
	)

	private val CONFIG_PATTERN = Regex("""^nadle\.config\.[cm]?[jt]s$""")

	fun scanTasks(rootDir: Path): List<String> =
		scanTasksDetailed(rootDir).map { it.name }.sorted()

	fun scanTasksDetailed(rootDir: Path): List<NadleTask> {
		if (!Files.isDirectory(rootDir)) return emptyList()

		val tasks = mutableListOf<NadleTask>()

		Files.walkFileTree(rootDir, object : SimpleFileVisitor<Path>() {
			override fun preVisitDirectory(
				dir: Path,
				attrs: BasicFileAttributes
			): FileVisitResult {
				if (dir != rootDir && SKIP_DIRS.contains(dir.fileName.toString())) {
					return FileVisitResult.SKIP_SUBTREE
				}
				return FileVisitResult.CONTINUE
			}

			override fun visitFile(
				file: Path,
				attrs: BasicFileAttributes
			): FileVisitResult {
				if (CONFIG_PATTERN.matches(file.fileName.toString())) {
					val text = try {
						Files.readString(file)
					} catch (_: IOException) {
						return FileVisitResult.CONTINUE
					}

					val names = NadleFileUtil.extractAllTaskNames(text)
					val relDir = rootDir.relativize(file.parent)

					for (name in names) {
						val qualifiedName = if (relDir.toString().isEmpty()) {
							name
						} else {
							val prefix = relDir.toString()
								.replace(File.separatorChar, ':')
							":$prefix:$name"
						}
						tasks.add(
							NadleTask(
								name = qualifiedName,
								configFilePath = file.toAbsolutePath(),
								workingDirectory = file.parent.toAbsolutePath()
							)
						)
					}
				}
				return FileVisitResult.CONTINUE
			}

			override fun visitFileFailed(
				file: Path,
				exc: IOException?
			): FileVisitResult = FileVisitResult.CONTINUE
		})

		return tasks
	}
}
