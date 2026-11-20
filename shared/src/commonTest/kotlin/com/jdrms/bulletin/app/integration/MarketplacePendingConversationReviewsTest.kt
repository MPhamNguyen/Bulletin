package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequest
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryReviewRequestRepository
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarketplacePendingConversationReviewsTest {
    @Test
    fun pendingAndAcceptedRequestsDisappearAfterSubmission() = runTest {
        val requests = InMemoryReviewRequestRepository()
        val adapter = MarketplacePendingConversationReviews(requests)
        requests.createIfMissing(ReviewRequest(MarketplaceItemId("1"), "buyer", "seller", "Desk"))
        assertEquals("1", adapter.forBuyer(SenderId("buyer")).getOrThrow().single().listingId.value)
        requests.updateStatus("1", "buyer", ReviewRequestStatus.ACCEPTED)
        assertEquals(1, adapter.forBuyer(SenderId("buyer")).getOrThrow().size)
        assertTrue(adapter.forBuyer(SenderId("other")).getOrThrow().isEmpty())
        requests.updateStatus("1", "buyer", ReviewRequestStatus.SUBMITTED)
        assertTrue(adapter.forBuyer(SenderId("buyer")).getOrThrow().isEmpty())
    }
}
