package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingCondition

enum class ListingsLoadState {
    INITIAL,
    LOADING,
    LOADED,
    FAILED
}

data class ListingsUiState(
    val myListings: List<Listing> = emptyList(),
    val loadState: ListingsLoadState = ListingsLoadState.INITIAL,
    val newTitle: String = "",
    val newDescription: String = "",
    val newPrice: String = "",
    val newCategory: ListingCategory = ListingCategory.TEXTBOOKS,
    val newCondition: ListingCondition = ListingCondition.GOOD,
    val isSubmitting: Boolean = false,
    val successMessage: String? = null,
    val errorMessage: String? = null,
    val editingListing: Listing? = null,
    val editTitle: String = "",
    val editDescription: String = "",
    val editPrice: String = "",
    val editCategory: ListingCategory = ListingCategory.TEXTBOOKS,
    val editCondition: ListingCondition = ListingCondition.GOOD,
    val isUpdating: Boolean = false
)
