package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerProfile
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.service.MarketplaceItemSortingPolicy
import com.jdrms.bulletin.domain.marketplace.domain.service.MarketplaceSearchPolicy

data class SellerProfileUiState(
    val preview: SellerProfilePreview? = null,
    val profile: MarketplaceSellerProfile? = null,
    val listings: List<MarketplaceItem> = emptyList(),
    val filters: MarketplaceFilterState = MarketplaceFilterState(),
    val soldListingsCount: Int = 0,
    val isLoadingMoreReviews: Boolean = false,
    val reviewLoadError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    val filteredListings: List<MarketplaceItem>
        get() = sellerProfileSortingPolicy.sort(
            sellerProfileSearchPolicy.filterItems(
                items = listings,
                query = filters.query,
                category = filters.category,
                condition = filters.condition,
                attributes = filters.attributes,
                priceRange = filters.priceRange
            ),
            filters.sortOrder
        )
}

private val sellerProfileSearchPolicy = MarketplaceSearchPolicy()
private val sellerProfileSortingPolicy = MarketplaceItemSortingPolicy()
