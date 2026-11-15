package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeValue
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributes
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePrice
import com.jdrms.bulletin.domain.marketplace.domain.service.MarketplaceSearchPolicy
import kotlin.test.Test
import kotlin.test.assertEquals

class MarketplaceBrandSearchTest {
    private val attributes = MarketplaceAttributes(
        mapOf("brand" to MarketplaceAttributeValue.Text("Levi's"))
    )

    @Test
    fun listingSourceSearchMatchesNormalizedClothingBrand() {
        val listing = MarketplaceListingSnapshot(
            id = "jacket",
            sellerId = "seller",
            sellerName = "Seller",
            title = "Denim jacket",
            description = "Description",
            priceAmount = 25.0,
            priceCurrency = "USD",
            category = MarketplaceCategory.CLOTHING,
            createdAtMillis = 1L,
            attributes = attributes
        )

        assertEquals(
            listOf("jacket"),
            listOf(listing).pageFor(MarketplacePageRequest(query = "levis")).listings.map { it.id }
        )
        assertEquals(
            emptyList(),
            listOf(listing).pageFor(MarketplacePageRequest(query = "Nike")).listings
        )
    }

    @Test
    fun sellerProfileSearchUsesTheSameBrandNormalization() {
        val item = MarketplaceItem(
            id = MarketplaceItemId("jacket"),
            sellerId = "seller",
            sellerName = "Seller",
            title = "Denim jacket",
            description = "Description",
            price = MarketplacePrice(25.0),
            category = MarketplaceCategory.CLOTHING,
            attributes = attributes
        )

        assertEquals(
            listOf(item),
            MarketplaceSearchPolicy().filterItems(listOf(item), "LEVI\u2019S", MarketplaceCategory.CLOTHING)
        )
    }
}
