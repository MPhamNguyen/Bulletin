package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.domain.model.Category
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingId

data class MarketplaceUiState(
    val listings: List<Listing> = emptyList(),
    val favoriteIds: Set<ListingId> = emptySet(),
    val searchQuery: String = "",
    val selectedCategory: Category? = null,
    val selectedListing: Listing? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showCreateDialog: Boolean = false,

    // Form inputs for creating listing
    val newTitle: String = "",
    val newDescription: String = "",
    val newPrice: String = "",
    val newCategory: Category = Category.TEXTBOOKS
)
