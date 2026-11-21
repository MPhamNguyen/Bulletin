package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingPage
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSnapshot
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

class CompositeMarketplaceListingSourceTest {
    /** Each source waits for the other to start, so the test only completes if both are queried concurrently. */
    @Test
    fun sources_are_queried_concurrently_and_merged_newest_first() = runTest {
        val firstStarted = CompletableDeferred<Unit>()
        val secondStarted = CompletableDeferred<Unit>()
        val composite = CompositeMarketplaceListingSource(
            MarketplaceListingSource {
                firstStarted.complete(Unit)
                secondStarted.await()
                MarketplaceListingPage(listOf(listing("older", createdAtMillis = 100)), null)
            },
            MarketplaceListingSource {
                secondStarted.complete(Unit)
                firstStarted.await()
                MarketplaceListingPage(listOf(listing("newer", createdAtMillis = 200)), null)
            }
        )

        val page = withTimeout(1_000) { composite.getAvailableListings(MarketplacePageRequest(pageSize = 5)) }

        assertEquals(listOf("newer", "older"), page.listings.map { it.id })
    }

    private fun listing(id: String, createdAtMillis: Long) = MarketplaceListingSnapshot(
        id = id,
        sellerId = "seller",
        sellerName = "Seller",
        title = id,
        description = "Good condition",
        priceAmount = 25.0,
        priceCurrency = "USD",
        category = MarketplaceCategory.TEXTBOOKS,
        createdAtMillis = createdAtMillis
    )
}
