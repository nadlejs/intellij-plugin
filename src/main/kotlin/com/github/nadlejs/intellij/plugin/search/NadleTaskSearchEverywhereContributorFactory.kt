package com.github.nadlejs.intellij.plugin.search

import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributor
import com.intellij.ide.actions.searcheverywhere.SearchEverywhereContributorFactory
import com.intellij.openapi.actionSystem.AnActionEvent

class NadleTaskSearchEverywhereContributorFactory
	: SearchEverywhereContributorFactory<Any> {

	override fun createContributor(
		initEvent: AnActionEvent
	): SearchEverywhereContributor<Any> {
		val project = initEvent.project
			?: throw IllegalStateException("Project is required")
		@Suppress("UNCHECKED_CAST")
		return NadleTaskSearchEverywhereContributor(project)
			as SearchEverywhereContributor<Any>
	}
}
