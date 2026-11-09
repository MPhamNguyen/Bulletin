package com.jdrms.bulletin.domain.marketplace

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.SubmitMarketplaceReview
import com.jdrms.bulletin.domain.marketplace.application.ViewMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePrice
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequest
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.MarketplaceListingDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryReviewRequestRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class MarketplaceReviewTest {
    @Test
    fun listingReviewLookupUsesCanonicalListingId() = runTest {
        val repository = InMemoryMarketplaceRepository()
        val submitReview = SubmitMarketplaceReview(repository)
        val viewListing = ViewMarketplaceListing(repository)
        val listing = soldListing(buyerId = "buyer-1")

        assertIs<Result.Success<Unit>>(submitReview(listing, "buyer-1", 5, "Smooth transaction"))

        assertTrue(viewListing.hasReview("listing:listing-1", "buyer-1"))
        assertFalse(viewListing.hasReview("listing:listing-1", "buyer-2"))
    }

    @Test
    fun buyerCanReviewSoldListingAndCannotSubmitTwice() = runTest {
        val useCase = SubmitMarketplaceReview(InMemoryMarketplaceRepository())
        val listing = soldListing(buyerId = "buyer-1")

        assertIs<Result.Success<Unit>>(useCase(listing, "buyer-1", 5, "Smooth transaction"))
        val duplicate = useCase(listing, "buyer-1", 4, "Another comment")

        assertIs<Result.Error>(duplicate)
        assertEquals("A review has already been submitted for this listing.", duplicate.exception.message)
    }

    @Test
    fun reviewRequiresTheRecordedBuyerAndSoldState() = runTest {
        val useCase = SubmitMarketplaceReview(InMemoryMarketplaceRepository())
        val listing = soldListing(buyerId = "buyer-1")

        val wrongBuyer = useCase(listing, "buyer-2", 5, "Comment")
        val unsold = useCase(listing.copy(isSold = false), "buyer-1", 5, "Comment")

        assertEquals("Only the buyer can review this seller.", assertIs<Result.Error>(wrongBuyer).exception.message)
        assertEquals("Reviews are available after a listing is sold.", assertIs<Result.Error>(unsold).exception.message)
    }

    @Test
    fun reviewRequiresAComment() = runTest {
        val useCase = SubmitMarketplaceReview(repositoryWithListing(status = "SOLD", buyerId = "buyer-1"))
        val result = useCase.submitForSoldListing(
            listingId = "listing-1",
            buyerId = "buyer-1",
            sellerId = "seller-1",
            reviewerId = "buyer-1",
            score = 5,
            comment = "   "
        )

        assertEquals("Review comment cannot be empty.", assertIs<Result.Error>(result).exception.message)
    }

    @Test
    fun reviewRequestCannotSubmitAfterListingWasRestored() = runTest {
        val repository = repositoryWithListing(status = "AVAILABLE", buyerId = null)
        val useCase = SubmitMarketplaceReview(repository)

        val result = useCase.submitForSoldListing(
            listingId = "listing-1",
            buyerId = "buyer-1",
            sellerId = "seller-1",
            reviewerId = "buyer-1",
            score = 5,
            comment = "Smooth transaction"
        )

        assertEquals(
            "Reviews are available after a listing is sold.",
            assertIs<Result.Error>(result).exception.message
        )
        assertFalse(repository.hasReviewForListing(MarketplaceItemId("listing-1"), "buyer-1"))
    }

    @Test
    fun reviewRequestCannotSubmitWhenListingNowRecordsAnotherBuyer() = runTest {
        val repository = repositoryWithListing(status = "SOLD", buyerId = "buyer-2")
        val useCase = SubmitMarketplaceReview(repository)

        val result = useCase.submitForSoldListing(
            listingId = "listing-1",
            buyerId = "buyer-1",
            sellerId = "seller-1",
            reviewerId = "buyer-1",
            score = 5,
            comment = "Smooth transaction"
        )

        assertEquals("Only the buyer can review this seller.", assertIs<Result.Error>(result).exception.message)
        assertFalse(repository.hasReviewForListing(MarketplaceItemId("listing-1"), "buyer-1"))
    }

    @Test
    fun existingListingReviewFlowCompletesItsReviewRequest() = runTest {
        val requests = InMemoryReviewRequestRepository()
        requests.createIfMissing(
            ReviewRequest(MarketplaceItemId("listing-1"), "buyer-1", "seller-1", "Sold textbook")
        )
        val useCase = SubmitMarketplaceReview(InMemoryMarketplaceRepository(), requests)

        assertIs<Result.Success<Unit>>(useCase(soldListing("buyer-1"), "buyer-1", 5, "Smooth transaction"))
        assertEquals(emptyList(), requests.pendingForBuyer("buyer-1"))
    }

    private fun soldListing(buyerId: String): Listing = Listing(
        id = MarketplaceItemId("listing:listing-1"),
        sellerId = "seller-1",
        buyerId = buyerId,
        sellerName = "Seller",
        title = "Sold textbook",
        description = "A used textbook",
        price = MarketplacePrice(10.0),
        category = com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory.TEXTBOOKS,
        status = "SOLD",
        isSold = true
    )

    private fun repositoryWithListing(status: String, buyerId: String?) = InMemoryMarketplaceRepository(
        initialListings = listOf(
            MarketplaceListingDto(
                id = "listing-1",
                sellerId = "seller-1",
                buyerId = buyerId,
                sellerName = "Seller",
                title = "Sold textbook",
                description = "A used textbook",
                price = 10.0,
                category = "TEXTBOOKS",
                status = status
            )
        )
    )
}
