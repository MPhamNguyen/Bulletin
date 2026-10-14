package com.jdrms.bulletin.domain.marketplace

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.SubmitMarketplaceReview
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePrice
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class MarketplaceReviewTest {
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
}
