package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId

data class MarketplaceUiState(
    val items: List<MarketplaceItem> = emptyList(),
    val savedItemIds: Set<MarketplaceItemId> = emptySet(),
    val searchQuery: String = "",
    val selectedCategory: MarketplaceCategory? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val selectedListingId: String? = null,
    val selectedListing: Listing? = null,
    val isDetailLoading: Boolean = false,
    val detailErrorMessage: String? = null,
    val isDetailSheetOpen: Boolean = false
)
