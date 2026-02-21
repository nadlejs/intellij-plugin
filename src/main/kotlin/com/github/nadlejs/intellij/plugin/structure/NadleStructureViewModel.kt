package com.github.nadlejs.intellij.plugin.structure

import com.github.nadlejs.intellij.plugin.util.NadleFileUtil
import com.github.nadlejs.intellij.plugin.util.NadleIcons
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.structureView.TextEditorBasedStructureViewModel
import com.intellij.ide.util.treeView.smartTree.Sorter
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile
import javax.swing.Icon

class NadleStructureViewModel(
	private val psiFile: PsiFile,
	editor: Editor?
) : TextEditorBasedStructureViewModel(editor, psiFile) {

	override fun getSorters(): Array<Sorter> = arrayOf(Sorter.ALPHA_SORTER)

	override fun getRoot(): StructureViewTreeElement =
		NadleFileStructureViewElement(psiFile)

	private class NadleFileStructureViewElement(
		private val file: PsiFile
	) : StructureViewTreeElement, SortableTreeElement {

		override fun getValue(): Any = file

		override fun navigate(requestFocus: Boolean) {
			file.navigate(requestFocus)
		}

		override fun canNavigate(): Boolean = file.canNavigate()

		override fun canNavigateToSource(): Boolean = file.canNavigateToSource()

		override fun getAlphaSortKey(): String = file.name

		override fun getPresentation(): ItemPresentation = object : ItemPresentation {
			override fun getPresentableText(): String = file.name
			override fun getIcon(unused: Boolean): Icon = NadleIcons.Nadle
			override fun getLocationString(): String? = null
		}

		override fun getChildren(): Array<StructureViewTreeElement> {
			val text = file.text
			val results = mutableListOf<StructureViewTreeElement>()

			for (match in NadleFileUtil.TASK_REGISTER_PATTERN.findAll(text)) {
				val taskName = match.groupValues[1]
				val offset = match.range.first
				val element = file.findElementAt(offset) ?: continue
				results.add(NadleTaskStructureViewElement(element, taskName))
			}

			return results.toTypedArray()
		}
	}
}
