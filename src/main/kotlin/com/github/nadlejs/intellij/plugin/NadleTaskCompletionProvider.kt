package com.github.nadlejs.intellij.plugin

import com.intellij.ui.TextFieldWithAutoCompletionListProvider
import javax.swing.Icon

class NadleTaskCompletionProvider(
	items: Collection<String> = emptyList()
) : TextFieldWithAutoCompletionListProvider<String>(items) {

	fun updateItems(items: Collection<String>) {
		setItems(items)
	}

	override fun getLookupString(item: String): String = item

	override fun getIcon(item: String): Icon = NadleIcons.Nadle
}
