package com.github.nadlejs.intellij.plugin

import java.io.File
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.BasicFileAttributes

object NadleTaskScanner {

	private val SKIP_DIRS = setOf(
		"node_modules", ".git", "dist", "build",
		".next", ".nuxt", "coverage", ".turbo", ".cache", "out"
	)

	private val CONFIG_PATTERN = Regex("""^nadle\.config\.[cm]?[jt]s$""")

	fun scanTasks(rootDir: Path): List<String> {
		if (!Files.isDirectory(rootDir)) return emptyList()

		val tasks = mutableListOf<String>()

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
						if (relDir.toString().isEmpty()) {
							tasks.add(name)
						} else {
							val prefix = relDir.toString()
								.replace(File.separatorChar, ':')
							tasks.add(":$prefix:$name")
						}
					}
				}
				return FileVisitResult.CONTINUE
			}

			override fun visitFileFailed(
				file: Path,
				exc: IOException?
			): FileVisitResult = FileVisitResult.CONTINUE
		})

		return tasks.sorted()
	}
}
