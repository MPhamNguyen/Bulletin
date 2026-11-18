package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.SubmitMarketplaceReview
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequest
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.MarketplaceListingDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryReviewRequestRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ReviewRequestRatingOnlyTest {
    @Test
    fun ratingOnlySubmissionCompletesTheAcceptedRequest() = runTest {
        val repository = repository()
        val requests = InMemoryReviewRequestRepository()
        requests.createIfMissing(request)
        requests.updateStatus("listing-1", "buyer", ReviewRequestStatus.ACCEPTED)
        val actions = ReviewRequestActions(requests, SubmitMarketplaceReview(repository))

        assertIs<Result.Success<Unit>>(actions.submit(request.accept(), 5, ""))
        assertTrue(repository.hasReviewForListing(request.listingId, "buyer"))
        assertTrue(requests.pendingForBuyer("buyer").isEmpty())
    }

    @Test
    fun missingRatingStillFailsAndKeepsTheRequestAvailable() = runTest {
        val repository = repository()
        val requests = InMemoryReviewRequestRepository()
        requests.createIfMissing(request)
        val actions = ReviewRequestActions(requests, SubmitMarketplaceReview(repository))

        assertIs<Result.Error>(actions.submit(request, 0, ""))
        assertFalse(repository.hasReviewForListing(request.listingId, "buyer"))
        assertTrue(requests.pendingForBuyer("buyer").isNotEmpty())
    }

    private fun repository() = InMemoryMarketplaceRepository(
        initialListings = listOf(
            MarketplaceListingDto(
                id = "listing-1", sellerId = "seller", buyerId = "buyer", sellerName = "Seller",
                title = "Textbook", description = "Sold textbook", price = 10.0, category = "TEXTBOOKS", status = "SOLD"
            )
        )
    )

    private val request = ReviewRequest(MarketplaceItemId("listing-1"), "buyer", "seller", "Textbook")
}
