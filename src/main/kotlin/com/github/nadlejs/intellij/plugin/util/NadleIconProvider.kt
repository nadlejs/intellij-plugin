package com.github.nadlejs.intellij.plugin.util

import com.intellij.ide.IconProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import javax.swing.Icon

class NadleIconProvider : IconProvider() {

	override fun getIcon(element: PsiElement, flags: Int): Icon? {
		if (element !is PsiFile) return null
		val virtualFile = element.virtualFile ?: return null
		if (!NadleFileUtil.isNadleConfigFile(virtualFile)) return null
		return NadleIcons.Nadle
	}
}
