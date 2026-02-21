package com.github.nadlejs.intellij.plugin

import com.intellij.codeInsight.navigation.actions.GotoDeclarationHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

class NadleTaskGotoDeclarationHandler : GotoDeclarationHandler {

	override fun getGotoDeclarationTargets(
		element: PsiElement?,
		offset: Int,
		editor: Editor?
	): Array<PsiElement>? {
		if (element == null) return null

		val file = element.containingFile ?: return null
		val virtualFile = file.virtualFile ?: return null
		if (!NadleFileUtil.isNadleConfigFile(virtualFile)) return null

		// Get the task name from the string literal under cursor
		val targetTaskName = extractStringContent(element) ?: return null

		// Check this string is inside a dependsOn context
		if (!isInsideDependsOn(element)) return null

		// Find the tasks.register("targetTaskName", ...) definition in the file
		val fileText = file.text
		val pattern = Regex("""tasks\.register\s*\(\s*['"]${Regex.escape(targetTaskName)}['"]""")
		val match = pattern.find(fileText) ?: return null

		val targetElement = file.findElementAt(match.range.first) ?: return null
		return arrayOf(targetElement)
	}

	private fun extractStringContent(element: PsiElement): String? {
		val text = element.text ?: return null

		// Handle leaf elements inside string literals
		// Walk up to find the string literal node
		var current: PsiElement? = element
		while (current != null) {
			val t = current.text
			if (t.length >= 2 &&
				((t.startsWith("\"") && t.endsWith("\"")) ||
					(t.startsWith("'") && t.endsWith("'")))
			) {
				return t.substring(1, t.length - 1).ifBlank { null }
			}
			// Don't walk too far
			if (current.textLength > 200) break
			current = current.parent
		}

		// The element itself might be a bare string content token
		if (text.isNotBlank() && !text.contains(" ") && !text.startsWith("{")) {
			return text
		}

		return null
	}

	private fun isInsideDependsOn(element: PsiElement): Boolean {
		// Walk up the PSI tree and check surrounding text for dependsOn pattern
		var current: PsiElement? = element.parent
		var depth = 0
		while (current != null && depth < 15) {
			val text = current.text
			if (text.contains("dependsOn")) {
				return true
			}
			// Stop if we've gone past the property assignment level
			if (text.length > 2000) break
			current = current.parent
			depth++
		}
		return false
	}
}
