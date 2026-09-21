package com.jdrms.bulletin.domain.marketplace

import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSnapshot
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.SearchMarketplace
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MarketplaceApplicationTest {

    @Test
    fun searchReadsNewlyPublishedListingsFromCurrentSource() = runTest {
        val source = MutableMarketplaceListingSource()
        val searchMarketplace = SearchMarketplace(
            repository = InMemoryMarketplaceRepository(initialItems = emptyList()),
            listingSource = source
        )

        assertEquals(emptyList(), searchMarketplace.search("lamp", null))

        source.listings += publishedListing(title = "Adjustable Desk Lamp")

        val titleResults = searchMarketplace.search("desk lamp", null)
        assertEquals(listOf("Adjustable Desk Lamp"), titleResults.map { it.title })
        assertEquals("listing:list_42", titleResults.single().id.value)

        val categoryResults = searchMarketplace.search("furniture", null)
        assertEquals(listOf("Adjustable Desk Lamp"), categoryResults.map { it.title })
    }

    private fun publishedListing(title: String): MarketplaceListingSnapshot {
        return MarketplaceListingSnapshot(
            id = "listing:list_42",
            sellerId = "seller_42",
            sellerName = "Campus Seller",
            title = title,
            description = "LED lamp",
            priceAmount = 18.0,
            priceCurrency = "USD",
            category = MarketplaceCategory.FURNITURE,
            createdAtMillis = 42L
        )
    }
}

private class MutableMarketplaceListingSource : MarketplaceListingSource {
    val listings = mutableListOf<MarketplaceListingSnapshot>()

    override suspend fun getAvailableListings(): List<MarketplaceListingSnapshot> = listings.toList()
}
