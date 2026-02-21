package com.github.nadlejs.intellij.plugin.structure

import com.github.nadlejs.intellij.plugin.util.NadleIcons
import com.intellij.ide.structureView.StructureViewTreeElement
import com.intellij.ide.util.treeView.smartTree.SortableTreeElement
import com.intellij.navigation.ItemPresentation
import com.intellij.pom.Navigatable
import com.intellij.psi.PsiElement
import javax.swing.Icon

class NadleTaskStructureViewElement(
	private val element: PsiElement,
	private val taskName: String
) : StructureViewTreeElement, SortableTreeElement {

	override fun getValue(): Any = element

	override fun navigate(requestFocus: Boolean) {
		if (element is Navigatable) {
			(element as Navigatable).navigate(requestFocus)
		}
	}

	override fun canNavigate(): Boolean =
		element is Navigatable && (element as Navigatable).canNavigate()

	override fun canNavigateToSource(): Boolean =
		element is Navigatable && (element as Navigatable).canNavigateToSource()

	override fun getAlphaSortKey(): String = taskName

	override fun getPresentation(): ItemPresentation = object : ItemPresentation {
		override fun getPresentableText(): String = taskName
		override fun getIcon(unused: Boolean): Icon = NadleIcons.Nadle
		override fun getLocationString(): String? = null
	}

	override fun getChildren(): Array<StructureViewTreeElement> = emptyArray()
}
