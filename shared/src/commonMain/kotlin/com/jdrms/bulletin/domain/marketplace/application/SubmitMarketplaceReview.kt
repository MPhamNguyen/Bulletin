package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReview
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReviewId
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus
import com.jdrms.bulletin.domain.marketplace.domain.repository.MarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.domain.repository.ReviewRequestRepository

class SubmitMarketplaceReview(
    private val repository: MarketplaceRepository,
    private val reviewRequests: ReviewRequestRepository? = null
) {
    suspend fun submitForSoldListing(
        listingId: String,
        buyerId: String?,
        sellerId: String,
        reviewerId: String,
        score: Int,
        comment: String
    ): Result<Unit> {
        val canonicalListingId = MarketplaceItemId(listingId.removePrefix("listing:"))
        val currentListing = when (val result = repository.getListingForReview(canonicalListingId.value)) {
            is Result.Success -> result.data
            is Result.Error -> return Result.Error(
                IllegalStateException("Couldn't verify the listing's current status.", result.exception)
            )
        }
        reviewEligibilityError(currentListing, buyerId, sellerId, reviewerId, score, comment)?.let { error ->
            return Result.Error(IllegalArgumentException(error))
        }
        if (repository.hasReviewForListing(canonicalListingId, reviewerId)) {
            completeReviewRequest(canonicalListingId.value, reviewerId)
            return Result.Error(IllegalStateException("A review has already been submitted for this listing."))
        }
        val submission = repository.submitReview(
            MarketplaceReview(
                listingId = canonicalListingId,
                reviewerId = reviewerId,
                revieweeId = sellerId,
                score = score,
                comment = comment.trim()
            )
        )
        if (submission is Result.Success) completeReviewRequest(canonicalListingId.value, reviewerId)
        return submission
    }

    private fun reviewEligibilityError(
        listing: Listing,
        buyerId: String?,
        sellerId: String,
        reviewerId: String,
        score: Int,
        comment: String
    ): String? = when {
        !listing.isSold -> "Reviews are available after a listing is sold."
        buyerId != reviewerId || listing.buyerId != reviewerId -> "Only the buyer can review this seller."
        listing.sellerId != sellerId -> "The listing's seller has changed."
        comment.isBlank() -> "Review comment cannot be empty."
        score !in 1..5 -> "Choose a rating from 1 to 5 stars."
        else -> null
    }

    suspend operator fun invoke(
        listing: Listing,
        reviewerId: String,
        score: Int,
        comment: String
    ): Result<Unit> {
        val eligibilityError = when {
            !listing.isSold -> "Reviews are available after a listing is sold."
            listing.buyerId != reviewerId -> "Only the buyer can review this seller."
            score !in 1..5 -> "Choose a rating from 1 to 5 stars."
            else -> null
        }
        if (eligibilityError != null) return Result.Error(IllegalArgumentException(eligibilityError))
        if (repository.hasReviewForListing(listing.id, reviewerId)) {
            completeReviewRequest(listing.id.value.removePrefix("listing:"), reviewerId)
            return Result.Error(IllegalStateException("A review has already been submitted for this listing."))
        }

        val submission = repository.submitReview(
            MarketplaceReview(
                id = MarketplaceReviewId(""),
                listingId = MarketplaceItemId(listing.id.value.removePrefix("listing:")),
                reviewerId = reviewerId,
                revieweeId = listing.sellerId,
                score = score,
                comment = comment.trim()
            )
        )
        if (submission is Result.Success) {
            completeReviewRequest(listing.id.value.removePrefix("listing:"), reviewerId)
        }
        return submission
    }

    private suspend fun completeReviewRequest(listingId: String, buyerId: String) {
        reviewRequests?.updateStatus(listingId, buyerId, ReviewRequestStatus.SUBMITTED)
    }
}
