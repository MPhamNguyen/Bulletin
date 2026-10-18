package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.runtime.Composable
import com.jdrms.bulletin.core.designsystem.ListingFilterMenus
import com.jdrms.bulletin.core.designsystem.ListingFilterOption
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListing

@Composable
internal fun BookmarkedListingFilters(
    state: BookmarkedListingsViewState,
    actions: BookmarkedListingsActions
) {
    ListingFilterMenus(
        selectedCategory = state.filters.category,
        selectedCondition = state.filters.condition,
        categories = state.allListings.filterOptions { it.category },
        conditions = state.allListings.filterOptions { it.condition },
        onCategorySelected = actions.onCategorySelected,
        onConditionSelected = actions.onConditionSelected
    )
}

private fun List<ProfileBookmarkedListing>.filterOptions(
    value: (ProfileBookmarkedListing) -> String
): List<ListingFilterOption> = map(value)
    .filter(String::isNotBlank)
    .distinctBy(String::uppercase)
    .sorted()
    .map { option -> ListingFilterOption(option, option.toDisplayName()) }

private fun String.toDisplayName(): String = lowercase()
    .replace('_', ' ')
    .replaceFirstChar { it.uppercase() }
