package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePriceRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MarketplacePriceRangeTest {
    @Test
    fun filtersListingsUsingInclusivePriceBounds() {
        val listings = listOf(
            listing("below", 9.99),
            listing("minimum", 10.0),
            listing("inside", 19.99),
            listing("maximum", 20.0),
            listing("above", 20.01)
        )

        val page = listings.pageFor(
            MarketplacePageRequest(priceRange = MarketplacePriceRange(1_000L, 2_000L))
        )

        assertEquals(listOf("maximum", "inside", "minimum"), page.listings.map { it.id })
    }

    @Test
    fun rejectsMinimumAboveMaximum() {
        assertFailsWith<IllegalArgumentException> {
            MarketplacePriceRange(minimumCents = 2_001L, maximumCents = 2_000L)
        }
    }

    private fun listing(id: String, price: Double) = MarketplaceListingSnapshot(
        id = id,
        sellerId = "seller",
        sellerName = "Seller",
        title = "Listing $id",
        description = "Description",
        priceAmount = price,
        priceCurrency = "USD",
        category = MarketplaceCategory.OTHER,
        createdAtMillis = (price * 100).toLong()
    )
}
