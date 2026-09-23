package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingPrice
import com.jdrms.bulletin.domain.listings.domain.model.ListingStatus
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingPage
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSnapshot
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageCursor
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.application.pageFor
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ListingsMarketplaceListingSourceTest {

    @Test
    fun translatesNewAvailableListingsAndExcludesUnavailableListings() = runTest {
        val repository = InMemoryListingsRepository(initialListings = emptyList())
        repository.createListing(listing(id = "available", status = ListingStatus.AVAILABLE))
        repository.createListing(listing(id = "sold", status = ListingStatus.SOLD))

        val snapshots = ListingsMarketplaceListingSource(repository)
            .getAvailableListings(MarketplacePageRequest())
            .listings

        assertEquals(1, snapshots.size)
        assertEquals("listing:available", snapshots.single().id)
        assertEquals("Campus Sofa", snapshots.single().title)
        assertEquals(MarketplaceCategory.FURNITURE, snapshots.single().category)
    }

    @Test
    fun compositeSourceMergesStablePagesAcrossItsSources() = runTest {
        val source = CompositeMarketplaceListingSource(
            StaticMarketplaceListingSource(listOf(snapshot(5), snapshot(3), snapshot(1))),
            StaticMarketplaceListingSource(listOf(snapshot(4), snapshot(2)))
        )

        val firstPage = source.getAvailableListings(MarketplacePageRequest(pageSize = 3))
        val secondPage = source.getAvailableListings(
            MarketplacePageRequest(pageSize = 3, cursor = firstPage.nextCursor)
        )

        assertEquals(listOf("listing:5", "listing:4", "listing:3"), firstPage.listings.map { it.id })
        assertEquals(listOf("listing:2", "listing:1"), secondPage.listings.map { it.id })
        assertNull(secondPage.nextCursor)
    }

    @Test
    fun compositeSourceKeepsSourceCursorWhenMergedPageIsEmpty() = runTest {
        val continuation = MarketplacePageCursor(
            createdAt = kotlin.time.Instant.fromEpochMilliseconds(10L),
            itemId = "listing:continuation"
        )
        val source = CompositeMarketplaceListingSource(
            object : MarketplaceListingSource {
                override suspend fun getAvailableListings(
                    request: MarketplacePageRequest
                ): MarketplaceListingPage = MarketplaceListingPage(emptyList(), continuation)
            }
        )

        val page = source.getAvailableListings(MarketplacePageRequest())

        assertEquals(emptyList(), page.listings)
        assertNotNull(page.nextCursor)
        assertEquals(continuation, page.nextCursor)
    }

    private fun listing(id: String, status: ListingStatus): Listing {
        return Listing(
            id = ListingId(id),
            sellerId = SellerId("seller_1"),
            sellerName = "Campus Seller",
            title = "Campus Sofa",
            description = "Compact sofa in good condition",
            price = ListingPrice(75.0),
            category = ListingCategory.FURNITURE,
            status = status
        )
    }

    private fun snapshot(index: Long): MarketplaceListingSnapshot {
        return MarketplaceListingSnapshot(
            id = "listing:$index",
            sellerId = "seller_$index",
            sellerName = "Campus Seller",
            title = "Listing $index",
            description = "Description $index",
            priceAmount = index.toDouble(),
            priceCurrency = "USD",
            category = MarketplaceCategory.OTHER,
            createdAtMillis = index
        )
    }
}

private class StaticMarketplaceListingSource(
    private val listings: List<MarketplaceListingSnapshot>
) : MarketplaceListingSource {
    override suspend fun getAvailableListings(request: MarketplacePageRequest): MarketplaceListingPage {
        return listings.pageFor(request)
    }
}
