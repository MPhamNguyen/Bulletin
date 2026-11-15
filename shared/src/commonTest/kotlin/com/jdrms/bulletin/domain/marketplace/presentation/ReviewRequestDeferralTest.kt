package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequest
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class ReviewRequestDeferralTest {
    @Test
    fun deferredAcceptedRequestDoesNotReturnDuringTheCurrentSession() {
        val deferred = request("listing-1", ReviewRequestStatus.ACCEPTED)
        val next = request("listing-2")

        assertEquals(listOf(next), listOf(deferred, next).excludingDeferredReviewRequests(setOf("listing-1")))
    }

    private fun request(listingId: String, status: ReviewRequestStatus = ReviewRequestStatus.PENDING) =
        ReviewRequest(
            listingId = MarketplaceItemId(listingId),
            buyerId = "buyer-1",
            sellerId = "seller-1",
            listingTitle = "Listing",
            status = status
        )
}
