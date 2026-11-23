package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.app.application.GetVisibleMarketplaceListings
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceBlockedUserProvider
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSnapshot
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceReportedListingProvider
import com.jdrms.bulletin.domain.marketplace.application.pageFor
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals

class GetVisibleMarketplaceListingsConcurrencyTest {
    /**
     * Each collaborator announces that it started, then waits for the other two. The test only completes when all
     * three are in flight at once; a sequential implementation stalls and trips the timeout.
     */
    @Test
    fun visibility_lookups_and_first_page_are_requested_concurrently() = runTest {
        val blockedStarted = CompletableDeferred<Unit>()
        val reportedStarted = CompletableDeferred<Unit>()
        val pageStarted = CompletableDeferred<Unit>()
        val listings = listOf(
            listing("blocked", sellerId = "blocked-seller", createdAtMillis = 300),
            listing("reported", createdAtMillis = 200),
            listing("visible", createdAtMillis = 100)
        )
        val visibility = GetVisibleMarketplaceListings(
            listingSource = MarketplaceListingSource { request ->
                pageStarted.complete(Unit)
                blockedStarted.await()
                reportedStarted.await()
                listings.pageFor(request)
            },
            blockedUserProvider = MarketplaceBlockedUserProvider {
                blockedStarted.complete(Unit)
                reportedStarted.await()
                pageStarted.await()
                setOf("blocked-seller")
            },
            reportedListingProvider = MarketplaceReportedListingProvider {
                reportedStarted.complete(Unit)
                blockedStarted.await()
                pageStarted.await()
                setOf(MarketplaceItemId("reported"))
            }
        )

        val page = withTimeout(1_000) { visibility(MarketplacePageRequest(viewerId = "viewer", pageSize = 5)) }

        assertEquals(listOf("visible"), page.listings.map { it.id })
    }

    private fun listing(id: String, sellerId: String = "seller", createdAtMillis: Long) = MarketplaceListingSnapshot(
        id = id,
        sellerId = sellerId,
        sellerName = "Seller",
        title = id,
        description = "Good condition",
        priceAmount = 25.0,
        priceCurrency = "USD",
        category = MarketplaceCategory.TEXTBOOKS,
        createdAtMillis = createdAtMillis
    )
}
