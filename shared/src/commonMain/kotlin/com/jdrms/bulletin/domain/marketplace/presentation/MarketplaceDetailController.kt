package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.ViewMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class MarketplaceDetailController(
    private val state: MutableStateFlow<MarketplaceUiState>,
    private val scope: CoroutineScope,
    private val viewListing: ViewMarketplaceListing,
    private val currentUserId: suspend () -> String?
) {
    fun load(listingId: String) {
        scope.launch {
            when (val result = viewListing(listingId, currentUserId())) {
                is Result.Success -> applyListing(listingId, result.data)
                is Result.Error -> state.update {
                    it.copy(
                        detail = it.detail.copy(
                            isLoading = false,
                            errorMessage = result.exception.message ?: "Failed to load listing details."
                        )
                    )
                }
            }
        }
    }

    private suspend fun applyListing(listingId: String, listing: Listing) {
        val canonicalId = MarketplaceItemId(listingId)
        val isBookmarked = state.value.bookmarkedItemIds.contains(canonicalId)
        val userId = currentUserId()
        val hasReview = userId?.let { viewListing.hasReview(listing.id.value, it) } ?: false
        state.update {
            it.copy(
                detail = it.detail.copy(
                    listing = listing.copy(id = canonicalId, isBookmarked = isBookmarked),
                    isLoading = false,
                    errorMessage = null,
                    hasReview = hasReview
                ),
                currentUserId = userId
            )
        }
    }
}
