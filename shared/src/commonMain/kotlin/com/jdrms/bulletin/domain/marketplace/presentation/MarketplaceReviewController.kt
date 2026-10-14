package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.SubmitMarketplaceReview
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MarketplaceReviewController(
    private val state: MutableStateFlow<MarketplaceUiState>,
    private val submitMarketplaceReview: SubmitMarketplaceReview,
    private val scope: CoroutineScope
) {
    fun openReviewDialog() {
        state.update { it.copy(detail = it.detail.copy(isReviewDialogOpen = true, reviewErrorMessage = null)) }
    }

    fun dismissReviewDialog() {
        state.update { it.copy(detail = it.detail.copy(isReviewDialogOpen = false, reviewErrorMessage = null)) }
    }

    fun submitReview(score: Int, comment: String) {
        val detail = state.value.detail
        val listing = detail.listing ?: return
        val reviewerId = state.value.currentUserId ?: return
        state.update { it.copy(detail = detail.copy(isSubmittingReview = true, reviewErrorMessage = null)) }
        scope.launch {
            when (val result = submitMarketplaceReview(listing, reviewerId, score, comment)) {
                is Result.Success -> state.update {
                    it.copy(
                        detail = it.detail.copy(
                            hasReview = true,
                            isReviewDialogOpen = false,
                            isSubmittingReview = false
                        )
                    )
                }
                is Result.Error -> state.update {
                    it.copy(
                        detail = it.detail.copy(
                            isSubmittingReview = false,
                            reviewErrorMessage = result.exception.message
                        )
                    )
                }
            }
        }
    }
}
