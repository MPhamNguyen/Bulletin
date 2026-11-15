package com.jdrms.bulletin.domain.marketplace

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequest
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryReviewRequestRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ReviewRequestTest {
    @Test
    fun acceptedIsDistinctFromSubmittedAndCannotBeDeclined() {
        val request = request()
        val accepted = request.accept()
        assertEquals(ReviewRequestStatus.ACCEPTED, accepted.status)
        assertFailsWith<IllegalArgumentException> { accepted.decline() }
        assertEquals(ReviewRequestStatus.SUBMITTED, accepted.submitted().status)
    }

    @Test
    fun declineIsTerminal() {
        val declined = request().decline()
        assertFailsWith<IllegalArgumentException> { declined.accept() }
        assertFailsWith<IllegalArgumentException> { declined.submitted() }
    }

    @Test
    fun duplicateSaleRequestForListingAndBuyerKeepsOriginalStatus() = runTest {
        val repository = InMemoryReviewRequestRepository()
        repository.createIfMissing(request())
        repository.updateStatus("listing-1", "buyer-1", ReviewRequestStatus.DECLINED)
        repository.createIfMissing(request())
        assertEquals(emptyList(), repository.pendingForBuyer("buyer-1"))
    }

    @Test
    fun acceptedRequestRemainsVisibleForRetryAfterNextLogin() = runTest {
        val repository = InMemoryReviewRequestRepository()
        repository.createIfMissing(request())
        repository.updateStatus("listing-1", "buyer-1", ReviewRequestStatus.ACCEPTED)
        assertEquals(ReviewRequestStatus.ACCEPTED, repository.pendingForBuyer("buyer-1").single().status)
    }

    @Test
    fun restoreCancelsPendingAndAcceptedRequestsForThePriorBuyer() = runTest {
        val repository = InMemoryReviewRequestRepository()
        repository.createIfMissing(request("listing-1"))
        repository.createIfMissing(request("listing-2"))
        repository.updateStatus("listing-2", "buyer-1", ReviewRequestStatus.ACCEPTED)

        repository.cancelForRestoredListing("listing-1", "seller-1")
        assertEquals(listOf("listing-2"), repository.pendingForBuyer("buyer-1").map { it.listingId.value })

        repository.cancelForRestoredListing("listing-2", "seller-1")
        assertEquals(emptyList(), repository.pendingForBuyer("buyer-1"))
    }

    @Test
    fun cancelledRequestCanBeCreatedAgainForANewSaleToSameBuyer() = runTest {
        val repository = InMemoryReviewRequestRepository()
        repository.createIfMissing(request())
        repository.cancelForRestoredListing("listing-1", "seller-1")

        repository.createIfMissing(request())

        assertEquals(ReviewRequestStatus.PENDING, repository.pendingForBuyer("buyer-1").single().status)
    }

    private fun request(listingId: String = "listing-1") = ReviewRequest(
        MarketplaceItemId(listingId),
        "buyer-1",
        "seller-1",
        "Textbook"
    )
}
