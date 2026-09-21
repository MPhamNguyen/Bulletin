package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingPrice
import com.jdrms.bulletin.domain.listings.domain.model.ListingStatus
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ListingsMarketplaceListingSourceTest {

    @Test
    fun translatesNewAvailableListingsAndExcludesUnavailableListings() = runTest {
        val repository = InMemoryListingsRepository(initialListings = emptyList())
        repository.createListing(listing(id = "available", status = ListingStatus.AVAILABLE))
        repository.createListing(listing(id = "sold", status = ListingStatus.SOLD))

        val snapshots = ListingsMarketplaceListingSource(repository).getAvailableListings()

        assertEquals(1, snapshots.size)
        assertEquals("listing:available", snapshots.single().id)
        assertEquals("Campus Sofa", snapshots.single().title)
        assertEquals(MarketplaceCategory.FURNITURE, snapshots.single().category)
    }

    private fun listing(id: String, status: ListingStatus): Listing {
        return Listing(
            id = ListingId(id),
            sellerId = SellerId("seller_1"),
            sellerName = "Campus Seller",
            title = "Campus Sofa",
            description = "Compact sofa in good condition",
            price = ListingPrice(75.0),
            category = ListingCategory.FURNITURE,
            status = status
        )
    }
}
