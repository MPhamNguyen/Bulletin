package com.jdrms.bulletin.domain.marketplace

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.application.ViewMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSnapshot
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSource
import com.jdrms.bulletin.domain.marketplace.application.SearchMarketplace
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MarketplaceApplicationTest1 {

    private val repository = InMemoryMarketplaceRepository()
    private val viewMarketplaceListing = ViewMarketplaceListing(repository)

    @Test
    fun testViewListingMethodSignatureReturnsListing() = runTest {
        val listing = viewMarketplaceListing.viewListing("mkt_1")
        assertNotNull(listing)
        assertEquals("mkt_1", listing.id.value)
        assertEquals("Calculus: Early Transcendentals (8th Ed)", listing.title)
        assertEquals(35.0, listing.price.amount)
        assertEquals("Dominic Alfonso", listing.sellerName)
        assertEquals(4.8, listing.sellerReputationScore)
        assertTrue(listing.photos.isNotEmpty())
    }

    @Test
    fun testViewListingNotFoundThrowsException() = runTest {
        assertFailsWith<NoSuchElementException> {
            viewMarketplaceListing.viewListing("non_existent_id")
        }
    }

    @Test
    fun testInvokeOperatorReturnsResultSuccess() = runTest {
        val result = viewMarketplaceListing("mkt_2")
        assertTrue(result.isSuccess())
        val listing = (result as Result.Success).data
        assertEquals("mkt_2", listing.id.value)
        assertEquals("Sony WH-1000XM4 Noise Canceling Headphones", listing.title)
    }

    @Test
    fun testInvokeOperatorReturnsResultErrorWhenNotFound() = runTest {
        val result = viewMarketplaceListing("unknown_id")
        assertTrue(result.isError())
    }
}

class MarketplaceApplicationTest2 {

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
