package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceLayoutPreference
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageCursor
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId

data class MarketplaceUiState(
    val items: List<MarketplaceItem> = emptyList(),
    val bookmarkedItemIds: Set<MarketplaceItemId> = emptySet(),
    val filters: MarketplaceFilterState = MarketplaceFilterState(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val nextCursor: MarketplacePageCursor? = null,
    val endReached: Boolean = false,
    val errorMessage: String? = null,
    val bookmarkErrorMessage: String? = null,
    val layoutPreference: MarketplaceLayoutPreference = MarketplaceLayoutPreference.TWO_PER_ROW,
    val detail: MarketplaceDetailState = MarketplaceDetailState(),
    val currentUserId: String? = null
)

data class MarketplaceFilterState(
    val query: String = "",
    val category: MarketplaceCategory? = null,
    val condition: MarketplaceCondition? = null,
    val attributes: MarketplaceAttributeFilters = MarketplaceAttributeFilters.NONE
) {
    /** Attribute filters only make sense for Electronics; any other category clears them. */
    fun normalized(): MarketplaceFilterState =
        if (category == MarketplaceCategory.ELECTRONICS || attributes.isEmpty) {
            this
        } else {
            copy(attributes = MarketplaceAttributeFilters.NONE)
        }
}

data class MarketplaceDetailState(
    val listingId: String? = null,
    val listing: Listing? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isOpen: Boolean = false,
    val hasReview: Boolean = false,
    val isReviewDialogOpen: Boolean = false,
    val isSubmittingReview: Boolean = false,
    val reviewErrorMessage: String? = null
)
