package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MarketplaceHousingConditionTest {
    @Test
    fun housingCategoryRejectsAConditionFilter() {
        assertFailsWith<IllegalArgumentException> {
            MarketplacePageRequest(
                category = MarketplaceCategory.HOUSING,
                condition = MarketplaceCondition.GOOD
            )
        }
    }

    @Test
    fun conditionSearchAcrossAllCategoriesExcludesHousing() {
        val listings = listOf(
            listing("housing", MarketplaceCategory.HOUSING),
            listing("furniture", MarketplaceCategory.FURNITURE)
        )

        val result = listings.pageFor(MarketplacePageRequest(condition = MarketplaceCondition.GOOD))

        assertEquals(listOf("furniture"), result.listings.map { it.id })
    }

    private fun listing(id: String, category: MarketplaceCategory) = MarketplaceListingSnapshot(
        id = id,
        sellerId = "seller",
        sellerName = "Seller",
        title = id,
        description = "Description",
        priceAmount = 10.0,
        priceCurrency = "USD",
        category = category,
        createdAtMillis = 1L,
        condition = MarketplaceCondition.GOOD
    )
}
