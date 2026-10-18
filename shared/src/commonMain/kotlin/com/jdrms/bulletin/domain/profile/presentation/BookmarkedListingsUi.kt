package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListing
import com.jdrms.bulletin.domain.profile.application.ProfileBookmarkedListingFilterOptions

internal data class BookmarkedListingsViewState(
    val listings: List<ProfileBookmarkedListing>,
    val allListings: List<ProfileBookmarkedListing>,
    val filters: ProfileListingFilterState,
    val filterOptions: ProfileBookmarkedListingFilterOptions,
    val isLoading: Boolean,
    val isLoadingMore: Boolean,
    val hasMore: Boolean,
    val errorMessage: String?,
    val removingListingId: String?
) {
    val shouldShowEmptyState: Boolean
        get() = !isLoading && errorMessage == null && listings.isEmpty() && !hasMore
}

internal data class BookmarkedListingsActions(
    val onListingClick: (String) -> Unit,
    val onRemoveBookmark: (String) -> Unit,
    val onRefresh: () -> Unit,
    val onLoadMore: () -> Unit,
    val onCategorySelected: (String?) -> Unit,
    val onConditionSelected: (String?) -> Unit,
    val onBack: () -> Unit
)
