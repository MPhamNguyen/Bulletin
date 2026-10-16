package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.home.domain.model.HomeFeedCursor
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingPage
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSnapshot
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageCursor
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class MarketplaceHomeRepositoryTest {
    @Test
    fun maps_listing_pages_and_translates_cursor_for_the_next_request() = runTest {
        val cursor = MarketplacePageCursor(Instant.parse("2024-01-01T00:00:00.123456Z"), "listing:a")
        val source = RecordingMarketplaceListingSource(
            listOf(
                MarketplaceListingPage(
                    listings = listOf(snapshot("listing:a", "Book A")),
                    nextCursor = cursor
                ),
                MarketplaceListingPage(
                    listings = listOf(snapshot("listing:b", "Book B")),
                    nextCursor = null
                )
            )
        )
        val repository = MarketplaceHomeRepository(source)

        val first = repository.getFeed(userId = null, cursor = null, pageSize = 10)
        val second = repository.getFeed(userId = null, cursor = first.nextCursor, pageSize = 10)

        assertEquals("listing:a", first.items.single().id.value)
        assertEquals("Book A", first.items.single().title)
        assertEquals(10, source.requests.first().pageSize)
        assertEquals(HomeFeedCursor(cursor.createdAt, first.items.single().id), first.nextCursor)
        assertEquals(cursor, source.requests.last().cursor)
        assertEquals("listing:b", second.items.single().id.value)
        assertNull(second.nextCursor)
    }

    private fun snapshot(id: String, title: String) = MarketplaceListingSnapshot(
        id = id,
        sellerId = "seller",
        sellerName = "Seller",
        title = title,
        description = "Good condition",
        priceAmount = 25.0,
        priceCurrency = "USD",
        category = MarketplaceCategory.TEXTBOOKS,
        createdAtMillis = 100
    )
}

private class RecordingMarketplaceListingSource(
    private val pages: List<MarketplaceListingPage>
) : MarketplaceListingSource {
    val requests = mutableListOf<MarketplacePageRequest>()

    override suspend fun getAvailableListings(request: MarketplacePageRequest): MarketplaceListingPage {
        requests += request
        return pages[requests.lastIndex]
    }
}
