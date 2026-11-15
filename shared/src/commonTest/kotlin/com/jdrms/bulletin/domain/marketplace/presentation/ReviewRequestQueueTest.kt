package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequest
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class ReviewRequestQueueTest {
    @Test
    fun resolvingCurrentRequestAdvancesToTheNextQueuedRequest() {
        val first = request("listing-1")
        val second = request("listing-2")

        val remaining = advanceReviewRequestQueue(first, listOf(first, second))

        assertEquals(listOf(second), remaining)
        assertEquals(second, remaining.firstOrNull())
    }

    @Test
    fun refreshAppendsNewRequestsWithoutReorderingTheQueue() {
        val first = request("listing-1")
        val second = request("listing-2")

        assertEquals(listOf(first, second), mergeReviewRequestQueue(listOf(first), listOf(first, second)))
    }

    @Test
    fun refreshRemovesRequestsThatAreNoLongerActionable() {
        val current = request("listing-1")

        assertEquals(emptyList(), mergeReviewRequestQueue(listOf(current), emptyList()))
    }

    @Test
    fun refreshUsesPersistedAcceptedStatusForTheCurrentRequest() {
        val pending = request("listing-1")
        val accepted = request("listing-1", ReviewRequestStatus.ACCEPTED)

        assertEquals(listOf(accepted), mergeReviewRequestQueue(listOf(pending), listOf(accepted)))
    }

    private fun request(
        listingId: String,
        status: ReviewRequestStatus = ReviewRequestStatus.PENDING
    ) = ReviewRequest(
        listingId = MarketplaceItemId(listingId),
        buyerId = "buyer-1",
        sellerId = "seller-1",
        listingTitle = listingId,
        status = status
    )
}
