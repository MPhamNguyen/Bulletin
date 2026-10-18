package com.jdrms.bulletin.domain.marketplace

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceBlockedUserProvider
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingPage
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSnapshot
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageCursor
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceReportedListingProvider
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerProfile
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerProfileProvider
import com.jdrms.bulletin.domain.marketplace.application.SearchMarketplace
import com.jdrms.bulletin.domain.marketplace.application.ViewMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.application.pageFor
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReporterId
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class MarketplaceApplicationTest {

    private val repository = InMemoryMarketplaceRepository()
    private val viewMarketplaceListing = ViewMarketplaceListing(repository)

    @Test
    fun testViewListingMethodSignatureReturnsListing() = runTest {
        val listing = viewMarketplaceListing.viewListing("mkt_1")
        assertNotNull(listing)
        assertEquals("mkt_1", listing.id.value)
        assertEquals("Calculus: Early Transcendentals (8th Ed)", listing.title)
        assertEquals(35.0, listing.price.amount)
        assertEquals("Dominic Alfonso", listing.sellerName)
        assertEquals(4.8, listing.sellerReputationScore)
        assertTrue(listing.photos.isNotEmpty())
    }

    @Test
    fun testViewListingNotFoundThrowsException() = runTest {
        assertFailsWith<NoSuchElementException> {
            viewMarketplaceListing.viewListing("non_existent_id")
        }
    }

    @Test
    fun testInvokeOperatorReturnsResultSuccess() = runTest {
        val result = viewMarketplaceListing("mkt_2")
        assertTrue(result.isSuccess())
        val listing = (result as Result.Success).data
        assertEquals("mkt_2", listing.id.value)
        assertEquals("Sony WH-1000XM4 Noise Canceling Headphones", listing.title)
    }

    @Test
    fun viewListingUsesTheSellerProfileNameWhenItIsAvailable() = runTest {
        var profileRequests = 0
        val listingViewer = ViewMarketplaceListing(
            repository = repository,
            sellerProfileProvider = MarketplaceSellerProfileProvider { sellerId ->
                profileRequests++
                if (sellerId == "seller_101") {
                    MarketplaceSellerProfile(
                        name = "Alex Johnson",
                        school = "Example University",
                        avatarUrl = "https://example.com/alex.jpg",
                        major = "Computer Science",
                        reviewCount = 24
                    )
                } else {
                    null
                }
            }
        )

        val listing = listingViewer.viewListing("mkt_1")

        assertEquals("Alex Johnson", listing.sellerName)
        assertEquals("Example University", listing.sellerSchool)
        assertEquals("https://example.com/alex.jpg", listing.sellerAvatarUrl)
        assertEquals("Computer Science", listing.sellerMajor)
        assertEquals(24, listing.sellerReviewCount)
        assertEquals(1, profileRequests)
    }

    @Test
    fun testInvokeOperatorReturnsResultErrorWhenNotFound() = runTest {
        val result = viewMarketplaceListing("unknown_id")
        assertTrue(result.isError())
    }

    @Test
    fun searchReadsNewlyPublishedListingsFromCurrentSource() = runTest {
        val source = MutableMarketplaceListingSource()
        val searchMarketplace = SearchMarketplace(
            repository = InMemoryMarketplaceRepository(initialListings = emptyList()),
            listingSource = source
        )

        assertEquals(emptyList(), searchMarketplace.search("lamp", null))

        source.listings += publishedListing(title = "Adjustable Desk Lamp")

        val titleResults = searchMarketplace.search("desk lamp", null)
        assertEquals(listOf("Adjustable Desk Lamp"), titleResults.map { it.title })
        assertEquals("listing:list_42", titleResults.single().id.value)

        val categoryResults = searchMarketplace.search("furniture", null)
        assertEquals(listOf("Adjustable Desk Lamp"), categoryResults.map { it.title })
    }

    @Test
    fun searchFillsPageAfterSkippingHiddenListings() = runTest {
        val firstCursor = MarketplacePageCursor(Instant.fromEpochMilliseconds(1), "listing-2")
        val requests = mutableListOf<MarketplacePageRequest>()
        val source = MarketplaceListingSource { request ->
            requests += request
            if (request.cursor == null) {
                MarketplaceListingPage(
                    listings = listOf(snapshot("listing-1", "seller_101"), snapshot("listing-2", "seller_102")),
                    nextCursor = firstCursor
                )
            } else {
                MarketplaceListingPage(
                    listings = listOf(snapshot("listing-3", "seller_103")),
                    nextCursor = null
                )
            }
        }
        val search = SearchMarketplace(
            repository = InMemoryMarketplaceRepository(initialListings = emptyList()),
            listingSource = source,
            blockedUserProvider = MarketplaceBlockedUserProvider { setOf("seller_101") }
        )

        val page = search.getPage(MarketplacePageRequest(viewerId = "viewer", pageSize = 2))

        assertEquals(listOf("listing-2", "listing-3"), page.items.map { it.id.value })
        assertEquals(2, requests.size)
        assertEquals(firstCursor, requests[1].cursor)
        assertEquals(null, page.nextCursor)
    }

    @Test
    fun marketplaceSearchExcludesListingsOwnedByTheViewer() = runTest {
        val search = SearchMarketplace(
            repository = InMemoryMarketplaceRepository(initialListings = emptyList()),
            listingSource = MarketplaceListingSource {
                MarketplaceListingPage(
                    listings = listOf(snapshot("own", "viewer"), snapshot("other", "seller")),
                    nextCursor = null
                )
            }
        )

        val result = search.getPage(MarketplacePageRequest(viewerId = "viewer"))

        assertEquals(listOf("other"), result.items.map { it.id.value })
    fun searchHidesListingsReportedByTheCurrentViewer() = runTest {
        val source = MutableMarketplaceListingSource().apply {
            listings += listOf(snapshot("reported", "seller-1"), snapshot("visible", "seller-2"))
        }
        val search = SearchMarketplace(
            repository = InMemoryMarketplaceRepository(initialListings = emptyList()),
            listingSource = source,
            reportedListingProvider = MarketplaceReportedListingProvider { reporterId ->
                assertEquals(MarketplaceReporterId("viewer"), reporterId)
                setOf(MarketplaceItemId("reported"))
            }
        )

        val page = search.getPage(MarketplacePageRequest(viewerId = "viewer", pageSize = 2))

        assertEquals(listOf("visible"), page.items.map { it.id.value })
    }

    @Test
    fun viewingAReportedListingReturnsUnavailable() = runTest {
        val view = ViewMarketplaceListing(
            repository = repository,
            reportedListingProvider = MarketplaceReportedListingProvider {
                setOf(MarketplaceItemId("mkt_1"))
            }
        )

        val result = view("mkt_1", "viewer")

        assertIs<Result.Error>(result)
        assertEquals("This listing is unavailable.", result.message)
    }

    private fun snapshot(id: String, sellerId: String) = MarketplaceListingSnapshot(
        id = id,
        sellerId = sellerId,
        sellerName = sellerId,
        title = "Listing $id",
        description = "Description for $id",
        priceAmount = 10.0,
        priceCurrency = "USD",
        category = MarketplaceCategory.OTHER,
        createdAtMillis = 1L
    )

    @Test
    fun browseReturnsStablePagesWithoutLoadingTheWholeCatalog() = runTest {
        val source = MutableMarketplaceListingSource().apply {
            listings += (1L..5L).map { index ->
                publishedListing(title = "Listing $index").copy(
                    id = "listing:$index",
                    createdAtMillis = index
                )
            }
        }
        val searchMarketplace = SearchMarketplace(
            repository = InMemoryMarketplaceRepository(initialListings = emptyList()),
            listingSource = source
        )

        val firstPage = searchMarketplace.getPage(MarketplacePageRequest(pageSize = 2))
        val secondPage = searchMarketplace.getPage(
            MarketplacePageRequest(pageSize = 2, cursor = firstPage.nextCursor)
        )

        assertEquals(listOf("listing:5", "listing:4"), firstPage.items.map { it.id.value })
        assertEquals(listOf("listing:3", "listing:2"), secondPage.items.map { it.id.value })
        assertNotNull(secondPage.nextCursor)
    }

    @Test
    fun pageRequestRejectsSizesOutsideTheSupportedRange() {
        assertFailsWith<IllegalArgumentException> { MarketplacePageRequest(pageSize = 0) }
        assertFailsWith<IllegalArgumentException> { MarketplacePageRequest(pageSize = 51) }
    }

    private fun publishedListing(title: String): MarketplaceListingSnapshot {
        return MarketplaceListingSnapshot(
            id = "listing:list_42",
            sellerId = "seller_42",
            sellerName = "Campus Seller",
            title = title,
            description = "LED lamp",
            priceAmount = 18.0,
            priceCurrency = "USD",
            category = MarketplaceCategory.FURNITURE,
            createdAtMillis = 42L
        )
    }
}

private class MutableMarketplaceListingSource : MarketplaceListingSource {
    val listings = mutableListOf<MarketplaceListingSnapshot>()

    override suspend fun getAvailableListings(request: MarketplacePageRequest): MarketplaceListingPage {
        return listings.pageFor(request)
    }
}
