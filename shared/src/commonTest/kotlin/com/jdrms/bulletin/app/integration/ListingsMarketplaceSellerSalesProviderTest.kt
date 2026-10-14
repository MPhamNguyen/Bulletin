package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingCondition
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingPrice
import com.jdrms.bulletin.domain.listings.domain.model.ListingStatus
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceSellerId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ListingsMarketplaceSellerSalesProviderTest {
    @Test
    fun countSoldListingsCountsOnlyTheRequestedSellersCompletedSales() = runTest {
        val repository = SellerSalesListingsRepository(
            listOf(
                listing("sold-1", "seller-a", ListingStatus.SOLD),
                listing("sold-2", "seller-a", ListingStatus.SOLD),
                listing("available", "seller-a", ListingStatus.AVAILABLE),
                listing("other-seller", "seller-b", ListingStatus.SOLD)
            )
        )

        val count = ListingsMarketplaceSellerSalesProvider(repository)
            .countSoldListings(MarketplaceSellerId("seller-a"))

        assertEquals(2, count)
        assertEquals(SellerId("seller-a"), repository.requestedSellerId)
    }

    private fun listing(id: String, sellerId: String, status: ListingStatus) = Listing(
        id = ListingId(id),
        sellerId = SellerId(sellerId),
        sellerName = "Seller",
        title = "Title $id",
        description = "Description $id",
        price = ListingPrice(10.0),
        category = ListingCategory.OTHER,
        condition = ListingCondition.GOOD,
        status = status
    )
}

private class SellerSalesListingsRepository(
    private val listings: List<Listing>
) : ListingsRepository {
    var requestedSellerId: SellerId? = null
        private set

    override suspend fun createListing(listing: Listing): Result<Listing> = Result.Success(listing)
    override suspend fun updateListing(listing: Listing): Result<Listing> = Result.Success(listing)
    override suspend fun getListing(id: ListingId): Listing? = listings.firstOrNull { it.id == id }
    override suspend fun deleteListing(id: ListingId, sellerId: SellerId): Result<Unit> = Result.Success(Unit)
    override suspend fun getSellerListings(sellerId: SellerId): List<Listing> {
        requestedSellerId = sellerId
        return listings.filter { it.sellerId == sellerId }
    }
    override suspend fun getAllListings(): List<Listing> = listings
}
