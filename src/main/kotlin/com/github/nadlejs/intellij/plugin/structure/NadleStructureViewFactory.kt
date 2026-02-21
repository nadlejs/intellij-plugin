package com.github.nadlejs.intellij.plugin.structure

import com.github.nadlejs.intellij.plugin.util.NadleFileUtil
import com.intellij.ide.structureView.StructureViewBuilder
import com.intellij.ide.structureView.TreeBasedStructureViewBuilder
import com.intellij.ide.structureView.StructureViewModel
import com.intellij.lang.PsiStructureViewFactory
import com.intellij.openapi.editor.Editor
import com.intellij.psi.PsiFile

class NadleStructureViewFactory : PsiStructureViewFactory {

	override fun getStructureViewBuilder(psiFile: PsiFile): StructureViewBuilder? {
		val virtualFile = psiFile.virtualFile ?: return null
		if (!NadleFileUtil.isNadleConfigFile(virtualFile)) return null

		return object : TreeBasedStructureViewBuilder() {
			override fun createStructureViewModel(
				editor: Editor?
			): StructureViewModel =
				NadleStructureViewModel(psiFile, editor)
		}
	}
}
