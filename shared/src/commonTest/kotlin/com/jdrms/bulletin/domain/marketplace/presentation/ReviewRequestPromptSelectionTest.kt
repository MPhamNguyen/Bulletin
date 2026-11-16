package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequest
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus
import kotlin.test.Test
import kotlin.test.assertEquals

class ReviewRequestPromptSelectionTest {
    @Test
    fun requestIsSelectedWhenBuyerComesOnlineWithPendingWork() {
        val pending = request("listing-1")

        assertEquals(pending, selectReviewRequestToPresent(null, listOf(pending)))
    }

    @Test
    fun acceptedRequestIsRestoredWhenPromptStartsAgain() {
        val accepted = request("listing-1", ReviewRequestStatus.ACCEPTED)

        assertEquals(accepted, selectReviewRequestToPresent(null, listOf(accepted)))
    }

    @Test
    fun activeBuyerKeepsTheCurrentPromptWhenNewRequestsArrive() {
        val current = request("listing-1")
        val later = request("listing-2")

        assertEquals(current, selectReviewRequestToPresent(current, listOf(current, later)))
    }

    @Test
    fun cancelledCurrentRequestIsRemovedFromThePrompt() {
        val current = request("listing-1")
        val next = request("listing-2")

        assertEquals(next, selectReviewRequestToPresent(current, listOf(next)))
    }

    @Test
    fun terminalRequestsAreNeverPresented() {
        val submitted = request("listing-1", ReviewRequestStatus.SUBMITTED)
        val declined = request("listing-2", ReviewRequestStatus.DECLINED)
        val cancelled = request("listing-3", ReviewRequestStatus.CANCELLED)
        val pending = request("listing-4")

        assertEquals(null, selectReviewRequestToPresent(null, listOf(submitted, declined, cancelled)))
        assertEquals(pending, selectReviewRequestToPresent(submitted, listOf(submitted, pending)))
    }

    private fun request(
        listingId: String,
        status: ReviewRequestStatus = ReviewRequestStatus.PENDING
    ) = ReviewRequest(
        listingId = MarketplaceItemId(listingId),
        buyerId = "buyer-1",
        sellerId = "seller-1",
        listingTitle = "Listing",
        status = status
    )
}
