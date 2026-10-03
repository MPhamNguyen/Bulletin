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

class ListingsActiveListingsCountProviderTest {

    @Test
    fun returnsCountOfAvailableListingsForSpecificUser() = runTest {
        val targetSellerId = SellerId("user-123")
        val otherSellerId = SellerId("user-456")

        val listings = listOf(
            createTestListing("item-1", targetSellerId, ListingStatus.AVAILABLE),
            createTestListing("item-2", targetSellerId, ListingStatus.AVAILABLE),
            createTestListing("item-3", targetSellerId, ListingStatus.SOLD),
            createTestListing("item-4", targetSellerId, ListingStatus.ARCHIVED),
            createTestListing("item-5", targetSellerId, ListingStatus.PENDING),
            createTestListing("item-6", otherSellerId, ListingStatus.AVAILABLE)
        )
        val repository = FakeListingsRepository(listings)
        val provider = ListingsActiveListingsCountProvider(repository)

        val count = provider.getActiveListingsCount(UserId("user-123"))

        assertEquals(2, count)
    }

    @Test
    fun returnsZeroWhenUserHasNoListings() = runTest {
        val repository = FakeListingsRepository(emptyList())
        val provider = ListingsActiveListingsCountProvider(repository)

        val count = provider.getActiveListingsCount(UserId("empty-user"))

        assertEquals(0, count)
    }

    @Test
    fun returnsZeroWhenRepositoryThrowsException() = runTest {
        val repository = object : FakeListingsRepository(emptyList()) {
            override suspend fun getSellerListings(sellerId: SellerId): List<Listing> {
                error("Network failure")
            }
        }
        val provider = ListingsActiveListingsCountProvider(repository)

        val count = provider.getActiveListingsCount(UserId("user-123"))

        assertEquals(0, count)
    }

    private fun createTestListing(
        id: String,
        sellerId: SellerId,
        status: ListingStatus
    ): Listing {
        return Listing(
            id = ListingId(id),
            sellerId = sellerId,
            sellerName = "Seller",
            title = "Title $id",
            description = "Description $id",
            price = ListingPrice(10.0),
            category = ListingCategory.OTHER,
            condition = ListingCondition.GOOD,
            status = status,
            images = emptyList(),
            createdAtMillis = 0L
        )
    }
}

private open class FakeListingsRepository(
    private val listings: List<Listing>
) : ListingsRepository {
    override suspend fun createListing(listing: Listing): Result<Listing> = Result.Success(listing)
    override suspend fun updateListing(listing: Listing): Result<Listing> = Result.Success(listing)
    override suspend fun getListing(id: ListingId): Listing? = listings.firstOrNull { it.id == id }
    override suspend fun deleteListing(id: ListingId): Result<Unit> = Result.Success(Unit)
    override suspend fun getSellerListings(sellerId: SellerId): List<Listing> {
        return listings.filter { it.sellerId == sellerId }
    }
    override suspend fun getAllListings(): List<Listing> = listings
}
