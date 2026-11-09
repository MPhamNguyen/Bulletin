package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceSortOrder
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MarketplaceSortVisibilityTest {
    @Test
    fun hiddenListingsKeepPriceCursorAcrossPages() = runTest {
        val listings = listOf(
            listing("one", 1.0, "blocked"),
            listing("two", 2.0),
            listing("three", 3.0),
            listing("four", 4.0),
            listing("five", 5.0)
        )
        val source = MarketplaceListingSource { request -> listings.pageFor(request) }
        val search = SearchMarketplace(
            repository = InMemoryMarketplaceRepository(initialListings = emptyList()),
            listingSource = source,
            blockedUserProvider = MarketplaceBlockedUserProvider { setOf("blocked") }
        )
        val request = MarketplacePageRequest(
            viewerId = "viewer",
            sortOrder = MarketplaceSortOrder.PRICE_LOW_TO_HIGH,
            pageSize = 2
        )

        val first = search.getPage(request)
        val second = search.getPage(request.copy(cursor = first.nextCursor))

        assertEquals(listOf("two", "three", "four", "five"), (first.items + second.items).map { it.id.value })
    }

    private fun listing(
        id: String,
        price: Double,
        sellerId: String = "seller-$id"
    ) = MarketplaceListingSnapshot(
        id = id,
        sellerId = sellerId,
        sellerName = "Seller",
        title = "Listing $id",
        description = "Description",
        priceAmount = price,
        priceCurrency = "USD",
        category = MarketplaceCategory.OTHER,
        createdAtMillis = price.toLong()
    )
}
