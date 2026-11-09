package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.SubmitMarketplaceReview
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequest
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus
import com.jdrms.bulletin.domain.marketplace.domain.repository.ReviewRequestRepository

internal class ReviewRequestActions(
    private val requests: ReviewRequestRepository,
    private val submitReview: SubmitMarketplaceReview
) {
    suspend fun accept(request: ReviewRequest): Result<Unit> = requests.updateStatus(
        request.listingId.value,
        request.buyerId,
        ReviewRequestStatus.ACCEPTED
    )

    suspend fun decline(request: ReviewRequest): Result<Unit> = requests.updateStatus(
        request.listingId.value,
        request.buyerId,
        ReviewRequestStatus.DECLINED
    )

    suspend fun submit(request: ReviewRequest, score: Int, comment: String): Result<Unit> {
        val result = submitReview.submitForSoldListing(
            request.listingId.value,
            request.buyerId,
            request.sellerId,
            request.buyerId,
            score,
            comment
        )
        if (result is Result.Error && !result.exception.message.orEmpty()
                .contains("already been submitted", ignoreCase = true)
        ) {
            return result
        }
        return requests.updateStatus(request.listingId.value, request.buyerId, ReviewRequestStatus.SUBMITTED)
    }
}
