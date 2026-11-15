package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterest
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem

data class MarketplaceInterestsUiState(
    val interests: Map<MarketplaceCategory, MarketplaceInterest> = emptyMap(),
    val matchingListings: List<MarketplaceItem> = emptyList(),
    val unreadMatchCount: Int = 0,
    val isLoading: Boolean = false,
    val isLoadingMatches: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null
) {
    val selectedCategories: Set<MarketplaceCategory>
        get() = interests.keys
}
