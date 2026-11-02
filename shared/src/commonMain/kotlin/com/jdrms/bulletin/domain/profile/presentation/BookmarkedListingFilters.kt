package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.runtime.Composable
import com.jdrms.bulletin.core.designsystem.ListingFilterOption
import com.jdrms.bulletin.core.designsystem.ListingSearchFilterState
import com.jdrms.bulletin.core.designsystem.ListingSearchFilters

@Composable
internal fun BookmarkedListingFilters(
    state: BookmarkedListingsViewState,
    actions: BookmarkedListingsActions
) {
    ListingSearchFilters(
        state = ListingSearchFilterState(
            query = state.filters.query,
            category = state.filters.category,
            condition = state.filters.condition
        ),
        categories = state.filterOptions.categories.toFilterOptions(),
        conditions = state.filterOptions.conditions.toFilterOptions(),
        onQueryChanged = actions.onQueryChanged,
        onFiltersApplied = { filters ->
            actions.onFiltersApplied(filters.category, filters.condition)
        }
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
