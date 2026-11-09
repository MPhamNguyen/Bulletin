package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.CancelListingReviewRequests
import com.jdrms.bulletin.domain.listings.application.CurrentListingSellerProvider
import com.jdrms.bulletin.domain.listings.application.ListingReviewRequestCreator
import com.jdrms.bulletin.domain.listings.application.ListingSeller
import com.jdrms.bulletin.domain.listings.application.RecentListingBuyersProvider
import com.jdrms.bulletin.domain.listings.application.RestoreListingToMarketplace
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingPrice
import com.jdrms.bulletin.domain.listings.domain.model.ListingStatus
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class ListingReviewRequestCancellationTest {
    @Test
    fun failedReviewRequestCancellationCanBeRetriedAfterRestore() = runTest {
        val sellerId = SellerId("seller-1")
        val listing = testListing(sellerId).copy(status = ListingStatus.SOLD, buyerId = "buyer-1")
        val repository = InMemoryListingsRepository(initialListings = emptyList())
        repository.createListing(listing)
        var cancellationAttempts = 0
        val creator = object : ListingReviewRequestCreator {
            override suspend fun create(listing: Listing, buyerId: String) = Result.Success(Unit)

            override suspend fun cancelForRestoredListing(listingId: ListingId, sellerId: SellerId): Result<Unit> {
                cancellationAttempts++
                return if (cancellationAttempts == 1) {
                    Result.Error(IllegalStateException("offline"))
                } else {
                    Result.Success(Unit)
                }
            }
        }
        val state = kotlinx.coroutines.flow.MutableStateFlow(ListingsUiState(pendingRestoration = listing))
        val actions = ListingStatusActions(
            state = state,
            scope = this,
            sellerProvider = object : CurrentListingSellerProvider {
                override suspend fun getCurrentSeller() = Result.Success(ListingSeller(sellerId, "Seller"))
            },
            markListingSold = null,
            recentBuyers = RecentListingBuyersProvider { _, _ -> emptyList() },
            reviewRequestUseCases = ListingReviewRequestUseCases(null, CancelListingReviewRequests(creator)),
            restoreListing = RestoreListingToMarketplace(repository),
            callbacks = ListingActionCallbacks(null, {}, {})
        )

        actions.confirmRestore()
        advanceUntilIdle()

        assertEquals(ListingStatus.AVAILABLE, repository.getListing(listing.id)?.status)
        assertEquals(listing.id, state.value.pendingReviewRequestCancellation?.id)
        assertFalse(state.value.isCancellingReviewRequests)

        actions.retryCancelReviewRequests()
        advanceUntilIdle()

        assertEquals(2, cancellationAttempts)
        assertNull(state.value.pendingReviewRequestCancellation)
        assertNull(state.value.reviewRequestCancellationError)
    }

    private fun testListing(ownerId: SellerId) = Listing(
        id = ListingId("listing-review-retry"),
        sellerId = ownerId,
        sellerName = "Seller",
        title = "Used textbook",
        description = "A used textbook in good condition",
        price = ListingPrice(20.0),
        category = com.jdrms.bulletin.domain.listings.domain.model.ListingCategory.TEXTBOOKS,
        condition = com.jdrms.bulletin.domain.listings.domain.model.ListingCondition.GOOD,
        status = ListingStatus.AVAILABLE
    )
}
