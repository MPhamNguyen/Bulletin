package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.SubmitMarketplaceReview
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequest
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus
import com.jdrms.bulletin.domain.marketplace.domain.repository.ReviewRequestRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.MarketplaceListingDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryReviewRequestRepository
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MarketplaceConversationReviewsTest {
    @Test
    fun ratingOnlyReviewIsSavedAndCompletesPendingRequestAndRetryIsSafe() = runTest {
        val marketplace = marketplace()
        val requests = InMemoryReviewRequestRepository()
        requests.createIfMissing(ReviewRequest(MarketplaceItemId("1"), "buyer", "seller", "Desk"))
        val adapter = MarketplaceConversationReviews(SubmitMarketplaceReview(marketplace, requests), requests)
        repeat(2) {
            assertIs<Result.Success<Unit>>(adapter.submit(listing, buyer, seller, 5, ""))
        }
        assertTrue(marketplace.hasReviewForListing(MarketplaceItemId("1"), "buyer"))
        assertTrue(requests.pendingForBuyer("buyer").isEmpty())
    }

    @Test
    fun requestCompletionFailureCanBeRetriedAfterReviewWasSaved() = runTest {
        val marketplace = marketplace()
        val requests = InMemoryReviewRequestRepository()
        requests.createIfMissing(ReviewRequest(MarketplaceItemId("1"), "buyer", "seller", "Desk"))
        var fail = true
        val flaky = object : ReviewRequestRepository by requests {
            override suspend fun updateStatus(
                listingId: String,
                buyerId: String,
                status: ReviewRequestStatus
            ): Result<Unit> =
                if (fail) {
                    Result.Error(
                        IllegalStateException("Offline")
                    )
                } else {
                    requests.updateStatus(listingId, buyerId, status)
                }
        }
        val adapter = MarketplaceConversationReviews(SubmitMarketplaceReview(marketplace, flaky), flaky)
        assertIs<Result.Error>(adapter.submit(listing, buyer, seller, 5, ""))
        assertTrue(marketplace.hasReviewForListing(MarketplaceItemId("1"), "buyer"))
        fail = false
        assertIs<Result.Success<Unit>>(adapter.submit(listing, buyer, seller, 5, ""))
        assertTrue(requests.pendingForBuyer("buyer").isEmpty())
    }

    @Test
    fun restoredListingWrongBuyerAndInvalidRatingAreRejected() = runTest {
        val requests = InMemoryReviewRequestRepository()
        for ((status, reviewer, rating) in listOf(
            Triple("AVAILABLE", buyer, 5),
            Triple("SOLD", SenderId("other"), 5),
            Triple("SOLD", buyer, 0)
        )) {
            val marketplace = marketplace(status)
            val adapter = MarketplaceConversationReviews(SubmitMarketplaceReview(marketplace, requests), requests)
            assertIs<Result.Error>(adapter.submit(listing, reviewer, seller, rating, ""))
            assertEquals(false, marketplace.hasReviewForListing(MarketplaceItemId("1"), reviewer.value))
        }
    }

    private fun marketplace(status: String = "SOLD") = InMemoryMarketplaceRepository(
        initialListings = listOf(
            MarketplaceListingDto(
                id = "1", sellerId = "seller", buyerId = "buyer", sellerName = "Seller", title = "Desk",
                description = "Desk", price = 10.0, category = "FURNITURE", status = status
            )
        )
    )

    private val listing = ListingReferenceId("listing:1")
    private val buyer = SenderId("buyer")
    private val seller = SenderId("seller")
}
