package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceSortOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MarketplaceSortingTest {
    private val listings = listOf(
        listing("b", price = 20.0, createdAt = 3L),
        listing("a", price = 10.0, createdAt = 2L),
        listing("c", price = 20.0, createdAt = 1L)
    )

    @Test
    fun priceLowToHighIsStableAcrossPages() {
        val request = MarketplacePageRequest(
            sortOrder = MarketplaceSortOrder.PRICE_LOW_TO_HIGH,
            pageSize = 2
        )
        val first = listings.pageFor(request)
        val second = listings.pageFor(request.copy(cursor = first.nextCursor))

        assertEquals(listOf("a", "b", "c"), (first.listings + second.listings).map { it.id })
        assertNull(second.nextCursor)
    }

    @Test
    fun priceHighToLowUsesIdAsDeterministicTieBreaker() {
        val page = listings.pageFor(
            MarketplacePageRequest(sortOrder = MarketplaceSortOrder.PRICE_HIGH_TO_LOW)
        )

        assertEquals(listOf("c", "b", "a"), page.listings.map { it.id })
    }

    private fun listing(id: String, price: Double, createdAt: Long) = MarketplaceListingSnapshot(
        id = id,
        sellerId = "seller",
        sellerName = "Seller",
        title = "Listing $id",
        description = "Description",
        priceAmount = price,
        priceCurrency = "USD",
        category = MarketplaceCategory.OTHER,
        createdAtMillis = createdAt
    )
}
