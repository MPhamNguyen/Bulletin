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
import kotlin.test.assertTrue
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
        assertNull(first.items.single().imageUrl)
        assertEquals(10, source.requests.first().pageSize)
        assertEquals(HomeFeedCursor(cursor.createdAt, first.items.single().id), first.nextCursor)
        assertEquals(cursor, source.requests.last().cursor)
        assertEquals("listing:b", second.items.single().id.value)
        assertNull(second.nextCursor)
    }

    @Test
    fun carriesPositionZeroPhotoIntoHomeFeedItem() = runTest {
        val source = RecordingMarketplaceListingSource(
            listOf(
                MarketplaceListingPage(
                    listOf(
                        snapshot("photo", "Photo listing").copy(
                            primaryPhotoUrl = "https://storage.example/cover.webp",
                            photoUrls = listOf(
                                "https://storage.example/cover.webp",
                                "https://storage.example/second.webp"
                            )
                        )
                    ),
                    null
                )
            )
        )

        val feed = MarketplaceHomeRepository(source).getFeed(userId = null, cursor = null, pageSize = 10)

        assertEquals("https://storage.example/cover.webp", feed.items.single().imageUrl)
        assertEquals(2, feed.items.single().photoUrls.size)
    }

    @Test
    fun feed_keeps_newest_listing_first_when_older_listing_matches_preferences() = runTest {
        val source = RecordingMarketplaceListingSource(
            listOf(
                MarketplaceListingPage(
                    listings = listOf(
                        snapshot("newest", "Newest item", MarketplaceCategory.OTHER, 200),
                        snapshot("older", "Older book", MarketplaceCategory.TEXTBOOKS, 100)
                    ),
                    nextCursor = null
                )
            )
        )

        val feed = MarketplaceHomeRepository(source).getFeed(userId = null, cursor = null, pageSize = 10)

        assertEquals(listOf("newest", "older"), feed.items.map { it.id.value })
        assertTrue(feed.items.last().score > feed.items.first().score)
    }

    @Test
    fun feed_omits_listings_created_by_the_viewer() = runTest {
        val source = RecordingMarketplaceListingSource(
            listOf(
                MarketplaceListingPage(
                    listings = listOf(
                        snapshot("mine", "My listing", sellerId = "viewer"),
                        snapshot("theirs", "Another listing", sellerId = "seller")
                    ),
                    nextCursor = null
                )
            )
        )

        val feed = MarketplaceHomeRepository(source).getFeed("viewer", null, 10)

        assertEquals(listOf("theirs"), feed.items.map { it.id.value })
        assertEquals("viewer", source.requests.single().viewerId)
    }

    private fun snapshot(
        id: String,
        title: String,
        category: MarketplaceCategory = MarketplaceCategory.TEXTBOOKS,
        createdAtMillis: Long = 100,
        sellerId: String = "seller"
    ) = MarketplaceListingSnapshot(
        id = id,
        sellerId = sellerId,
        sellerName = "Seller",
        title = title,
        description = "Good condition",
        priceAmount = 25.0,
        priceCurrency = "USD",
        category = category,
        createdAtMillis = createdAtMillis
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
