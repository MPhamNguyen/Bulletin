package com.jdrms.bulletin.domain.listings.presentation.viewmodel

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.CurrentListingSellerProvider
import com.jdrms.bulletin.domain.listings.application.ListingSeller
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingCondition
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingPrice
import com.jdrms.bulletin.domain.listings.domain.model.ListingStatus
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import kotlinx.coroutines.delay

internal const val TEST_SELLER_NAME = "Dominic Student"
internal val TEST_SELLER_ID = SellerId("seller_123")

internal fun testListing(ownerId: SellerId): Listing = Listing(
    id = ListingId("list_test_vm"),
    sellerId = ownerId,
    sellerName = TEST_SELLER_NAME,
    title = "Tactile Keyboard",
    description = "Quiet tactile switches for studying",
    price = ListingPrice(40.0),
    category = ListingCategory.ELECTRONICS,
    condition = ListingCondition.GOOD,
    status = ListingStatus.AVAILABLE
)

internal class FakeListingSellerProvider(
    var currentSeller: Result<ListingSeller>
) : CurrentListingSellerProvider {
    override suspend fun getCurrentSeller(): Result<ListingSeller> = currentSeller
}

internal class CountingListingsRepository(
    private val delegate: ListingsRepository
) : ListingsRepository {
    var updateCount = 0
    var updateFailure: Throwable? = null
    var deleteCount = 0
    var deleteFailure: Throwable? = null
    var getListingFailure: Throwable? = null

    override suspend fun createListing(listing: Listing): Result<Listing> = delegate.createListing(listing)

    override suspend fun updateListing(listing: Listing): Result<Listing> {
        updateCount += 1
        delay(10)
        updateFailure?.let { return Result.Error(it) }
        return delegate.updateListing(listing)
    }

    override suspend fun getListing(id: ListingId): Listing? {
        getListingFailure?.let { throw it }
        return delegate.getListing(id)
    }

    override suspend fun deleteListing(id: ListingId, sellerId: SellerId): Result<Unit> {
        deleteCount += 1
        return deleteFailure?.let { Result.Error(it) } ?: delegate.deleteListing(id, sellerId)
    }

    override suspend fun getSellerListings(sellerId: SellerId): List<Listing> = delegate.getSellerListings(sellerId)
    override suspend fun getAllListings(): List<Listing> = delegate.getAllListings()
}
