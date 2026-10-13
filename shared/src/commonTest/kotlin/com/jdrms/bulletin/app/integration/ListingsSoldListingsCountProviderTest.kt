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
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ListingsSoldListingsCountProviderTest {
    @Test
    fun returnsCountOfSoldListingsForSpecificUser() = runTest {
        val targetSellerId = SellerId("user-123")
        val repository = FakeSoldCountListingsRepository(
            listOf(
                testListing("sold-1", targetSellerId, ListingStatus.SOLD),
                testListing("sold-2", targetSellerId, ListingStatus.SOLD),
                testListing("available", targetSellerId, ListingStatus.AVAILABLE),
                testListing("other-seller", SellerId("user-456"), ListingStatus.SOLD)
            )
        )

        val count = ListingsSoldListingsCountProvider(repository)
            .getSoldListingsCount(UserId("user-123"))

        assertEquals(2, count)
    }

    private fun testListing(id: String, sellerId: SellerId, status: ListingStatus) = Listing(
        id = ListingId(id),
        sellerId = sellerId,
        sellerName = "Seller",
        title = "Title $id",
        description = "Description $id",
        price = ListingPrice(10.0),
        category = ListingCategory.OTHER,
        condition = ListingCondition.GOOD,
        status = status
    )
}

private class FakeSoldCountListingsRepository(
    private val listings: List<Listing>
) : ListingsRepository {
    override suspend fun createListing(listing: Listing): Result<Listing> = Result.Success(listing)
    override suspend fun updateListing(listing: Listing): Result<Listing> = Result.Success(listing)
    override suspend fun getListing(id: ListingId): Listing? = listings.firstOrNull { it.id == id }
    override suspend fun deleteListing(id: ListingId, sellerId: SellerId): Result<Unit> = Result.Success(Unit)
    override suspend fun getSellerListings(sellerId: SellerId): List<Listing> =
        listings.filter { it.sellerId == sellerId }
    override suspend fun getAllListings(): List<Listing> = listings
}
