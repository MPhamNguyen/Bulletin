package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.SubmitMarketplaceReview
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus
import com.jdrms.bulletin.domain.marketplace.domain.repository.ReviewRequestRepository
import com.jdrms.bulletin.domain.messages.application.ConversationReviews
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

internal class MarketplaceConversationReviews(
    private val submitReview: SubmitMarketplaceReview,
    private val requests: ReviewRequestRepository
) : ConversationReviews {
    override suspend fun submit(
        listingId: ListingReferenceId,
        buyerId: SenderId,
        sellerId: SenderId,
        rating: Int,
        comment: String
    ): Result<Unit> {
        val result = submitReview.submitForSoldListing(
            listingId.value,
            buyerId.value,
            sellerId.value,
            buyerId.value,
            rating,
            comment
        )
        // A retry after a lost response should acknowledge the already saved review, as the old prompt did.
        if (result is Result.Error &&
            result.exception.message != "A review has already been submitted for this listing."
        ) {
            return result
        }
        return requests.updateStatus(
            listingId.value.removePrefix("listing:"),
            buyerId.value,
            ReviewRequestStatus.SUBMITTED
        )
    }
}
