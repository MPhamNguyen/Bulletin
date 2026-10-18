package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.runtime.Composable
import com.jdrms.bulletin.core.designsystem.ListingFilterMenus
import com.jdrms.bulletin.core.designsystem.ListingFilterOption

@Composable
internal fun BookmarkedListingFilters(
    state: BookmarkedListingsViewState,
    actions: BookmarkedListingsActions
) {
    ListingFilterMenus(
        selectedCategory = state.filters.category,
        selectedCondition = state.filters.condition,
        categories = state.filterOptions.categories.toFilterOptions(),
        conditions = state.filterOptions.conditions.toFilterOptions(),
        onCategorySelected = actions.onCategorySelected,
        onConditionSelected = actions.onConditionSelected
    )
}

private fun List<String>.toFilterOptions(): List<ListingFilterOption> = this
    .filter(String::isNotBlank)
    .distinctBy(String::uppercase)
    .sorted()
    .map { option -> ListingFilterOption(option, option.toDisplayName()) }

private fun String.toDisplayName(): String = lowercase()
    .replace('_', ' ')
    .replaceFirstChar { it.uppercase() }
