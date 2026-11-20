package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.CancelListingReviewRequests
import com.jdrms.bulletin.domain.listings.application.CreateListingReviewRequest
import com.jdrms.bulletin.domain.listings.application.CurrentListingSellerProvider
import com.jdrms.bulletin.domain.listings.application.ListingReviewRequestCreator
import com.jdrms.bulletin.domain.listings.application.ListingSeller
import com.jdrms.bulletin.domain.listings.application.MarkListingSold
import com.jdrms.bulletin.domain.listings.application.RestoreListingToMarketplace
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingPrice
import com.jdrms.bulletin.domain.listings.domain.model.ListingStatus
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class ListingsConversationListingSalesTest {
    @Test
    fun rawConversationIdCanBeSoldAndRestored() = runTest { verifySaleAndUndo("listing-1") }

    @Test
    fun prefixedMarketplaceIdCanBeSoldAndRestored() = runTest { verifySaleAndUndo("listing:listing-1") }

    @Test
    fun acceptingRawIdsDoesNotBypassOwnership() = runTest {
        val fixture = Fixture(SellerId("someone-else"))
        assertIs<Result.Error>(fixture.sales.markSold(ListingReferenceId("listing-1"), "buyer"))
        assertEquals(ListingStatus.AVAILABLE, fixture.repository.getListing(ListingId("listing-1"))?.status)
        assertEquals(emptyList(), fixture.reviewEvents)
    }

    private suspend fun verifySaleAndUndo(reference: String) {
        val fixture = Fixture()
        val id = ListingId("listing-1")
        assertIs<Result.Success<Unit>>(fixture.sales.markSold(ListingReferenceId(reference), "buyer"))
        assertEquals(ListingStatus.SOLD, fixture.repository.getListing(id)?.status)
        assertEquals("buyer", fixture.repository.getListing(id)?.buyerId)
        assertEquals(listOf("create:listing-1:buyer"), fixture.reviewEvents)
        assertIs<Result.Success<Unit>>(fixture.sales.undoSale(ListingReferenceId(reference)))
        assertEquals(ListingStatus.AVAILABLE, fixture.repository.getListing(id)?.status)
        assertNull(fixture.repository.getListing(id)?.buyerId)
        assertEquals(listOf("create:listing-1:buyer", "cancel:listing-1:seller"), fixture.reviewEvents)
    }

    private class Fixture(private val actingSeller: SellerId = SellerId("seller")) {
        val repository = InMemoryListingsRepository(initialListings = emptyList())
        val reviewEvents = mutableListOf<String>()
        private val sellerProvider = object : CurrentListingSellerProvider {
            override suspend fun getCurrentSeller(): Result<ListingSeller> {
                if (repository.getListing(ListingId("listing-1")) == null) {
                    repository.createListing(
                        Listing(
                            id = ListingId("listing-1"),
                            sellerId = SellerId("seller"),
                            sellerName = "Seller",
                            title = "Desk",
                            description = "A desk",
                            price = ListingPrice(10.0),
                            category = ListingCategory.FURNITURE
                        )
                    )
                }
                return Result.Success(ListingSeller(actingSeller, "Seller"))
            }
        }
        private val reviews = object : ListingReviewRequestCreator {
            override suspend fun create(listing: Listing, buyerId: String): Result<Unit> {
                reviewEvents += "create:${listing.id.value}:$buyerId"
                return Result.Success(Unit)
            }
            override suspend fun cancelForRestoredListing(listingId: ListingId, sellerId: SellerId): Result<Unit> {
                reviewEvents += "cancel:${listingId.value}:${sellerId.value}"
                return Result.Success(Unit)
            }
        }
        val sales = ListingsConversationListingSales(
            sellerProvider,
            MarkListingSold(repository),
            RestoreListingToMarketplace(repository),
            CreateListingReviewRequest(reviews),
            CancelListingReviewRequests(reviews)
        )
    }
}
