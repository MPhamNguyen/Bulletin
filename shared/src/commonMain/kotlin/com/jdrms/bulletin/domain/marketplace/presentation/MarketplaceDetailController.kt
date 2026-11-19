package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.LoadMarketplaceListingDetail
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Owns detail requests and prevents obsolete responses from replacing the active selection. */
internal class MarketplaceDetailController(
    private val state: MutableStateFlow<MarketplaceUiState>,
    private val scope: CoroutineScope,
    private val load: LoadMarketplaceListingDetail,
    private val currentUserId: suspend () -> String?
) {
    private var job: Job? = null
    private var generation = 0

    fun open(listingId: String) {
        state.update {
            it.copy(
                detail = MarketplaceDetailState(
                    listingId = listingId,
                    preview = it.items.firstOrNull { item -> item.id.value == listingId },
                    isLoading = true,
                    isOpen = true
                )
            )
        }
        retry()
    }

    fun retry() {
        val detail = state.value.detail
        val listingId = detail.listingId ?: return
        job?.cancel()
        val request = ++generation
        state.update { it.copy(detail = it.detail.copy(isLoading = true, errorMessage = null)) }
        job = scope.launch {
            val viewer = currentUserId()
            val result = load(listingId, viewer, detail.preview?.sellerId ?: detail.listing?.sellerId)
            state.update {
                if (request != generation) return@update it
                when (result) {
                    is Result.Success -> {
                        val id = MarketplaceItemId(listingId)
                        it.copy(
                            detail = it.detail.copy(
                                listing = result.data.listing.copy(id = id, isBookmarked = id in it.bookmarkedItemIds),
                                preview = null,
                                isLoading = false,
                                errorMessage = null,
                                hasReview = result.data.hasReview
                            ),
                            currentUserId = viewer
                        )
                    }
                    is Result.Error -> it.copy(
                        detail = it.detail.copy(
                            preview = null,
                            listing = null,
                            isLoading = false,
                            errorMessage = result.exception.message ?: "Failed to load listing details."
                        )
                    )
                }
            }
        }
    }

    fun dismiss() {
        generation++
        job?.cancel()
        state.update { it.copy(detail = MarketplaceDetailState()) }
    }
}
