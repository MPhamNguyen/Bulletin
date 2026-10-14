package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReview
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReviewId
import com.jdrms.bulletin.domain.marketplace.domain.repository.MarketplaceRepository

class SubmitMarketplaceReview(private val repository: MarketplaceRepository) {
    suspend fun submitForSoldListing(
        listingId: String,
        buyerId: String?,
        sellerId: String,
        isSold: Boolean,
        reviewerId: String,
        score: Int,
        comment: String
    ): Result<Unit> {
        val eligibilityError = when {
            !isSold -> "Reviews are available after a listing is sold."
            buyerId != reviewerId -> "Only the buyer can review this seller."
            else -> null
        }
        if (eligibilityError != null) return Result.Error(IllegalArgumentException(eligibilityError))
        if (comment.isBlank()) return Result.Error(IllegalArgumentException("Please add a comment to your review."))
        if (score !in 1..5) return Result.Error(IllegalArgumentException("Choose a rating from 1 to 5 stars."))
        val canonicalListingId = MarketplaceItemId(listingId.removePrefix("listing:"))
        if (repository.hasReviewForListing(canonicalListingId, reviewerId)) {
            return Result.Error(IllegalStateException("A review has already been submitted for this listing."))
        }
        return repository.submitReview(
            MarketplaceReview(
                listingId = canonicalListingId,
                reviewerId = reviewerId,
                revieweeId = sellerId,
                score = score,
                comment = comment.trim()
            )
        )
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
            comment.isBlank() -> "Please add a comment to your review."
            score !in 1..5 -> "Choose a rating from 1 to 5 stars."
            else -> null
        }
        if (eligibilityError != null) return Result.Error(IllegalArgumentException(eligibilityError))
        if (repository.hasReviewForListing(listing.id, reviewerId)) {
            return Result.Error(IllegalStateException("A review has already been submitted for this listing."))
        }

        return repository.submitReview(
            MarketplaceReview(
                id = MarketplaceReviewId(""),
                listingId = MarketplaceItemId(listing.id.value.removePrefix("listing:")),
                reviewerId = reviewerId,
                revieweeId = listing.sellerId,
                score = score,
                comment = comment.trim()
            )
        )
    }
}
