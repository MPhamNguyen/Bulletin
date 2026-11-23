package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.repository.MarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class LoadMarketplaceListingDetailTest {
    private val repository = SlowRepository(InMemoryMarketplaceRepository())
    private val profiles = SlowProfiles()

    private fun loader(
        blocked: MarketplaceBlockedUserProvider? = null
    ) = LoadMarketplaceListingDetail(
        ViewMarketplaceListing(repository, profiles, blocked)
    )

    @Test
    fun independentLookupsOverlapInsteadOfAddingUp() = runTest {
        val result = loader()("mkt_1", "viewer", sellerIdHint = SELLER_ID)

        assertIs<Result.Success<MarketplaceListingDetail>>(result)
        assertEquals(LOOKUP_MILLIS, testScheduler.currentTime)
        assertEquals("Resolved Seller", result.data.listing.sellerName)
    }

    @Test
    fun missingHintLooksUpTheProfileAfterTheListing() = runTest {
        val result = loader()("mkt_1", "viewer", sellerIdHint = null)

        assertIs<Result.Success<MarketplaceListingDetail>>(result)
        assertEquals(LOOKUP_MILLIS * 2, testScheduler.currentTime)
        assertEquals("Resolved Seller", result.data.listing.sellerName)
    }

    @Test
    fun mismatchedHintIsDiscardedAndTheRealSellerIsLookedUp() = runTest {
        loader()("mkt_1", "viewer", sellerIdHint = "someone_else")

        assertEquals(listOf("someone_else", SELLER_ID), profiles.requestedSellerIds)
    }

    @Test
    fun failedListingReturnsTheErrorWithoutWaitingForOtherLookups() = runTest {
        val result = loader()("missing", "viewer", sellerIdHint = SELLER_ID)

        assertIs<Result.Error>(result)
        assertEquals(LOOKUP_MILLIS, testScheduler.currentTime)
    }

    @Test
    fun blockedSellerMakesTheListingUnavailable() = runTest {
        val blocked = MarketplaceBlockedUserProvider { setOf(SELLER_ID) }

        val result = loader(blocked)("mkt_1", "viewer", sellerIdHint = SELLER_ID)

        assertIs<Result.Error>(result)
        assertEquals("This listing is unavailable.", result.message)
    }

    @Test
    fun anonymousViewerSkipsReviewAndVisibilityLookups() = runTest {
        val result = loader()("mkt_1", viewerId = null, sellerIdHint = SELLER_ID)

        assertIs<Result.Success<MarketplaceListingDetail>>(result)
        assertFalse(result.data.hasReview)
        assertEquals(0, repository.reviewChecks)
    }

    @Test
    fun reviewStateComesFromTheRepository() = runTest {
        val result = loader()("mkt_1", "viewer", sellerIdHint = SELLER_ID)

        assertIs<Result.Success<MarketplaceListingDetail>>(result)
        assertEquals(1, repository.reviewChecks)
        assertFalse(result.data.hasReview)
    }

    private class SlowRepository(
        private val delegate: MarketplaceRepository
    ) : MarketplaceRepository by delegate {
        var reviewChecks = 0

        override suspend fun viewListing(listingID: String): Result<Listing> {
            delay(LOOKUP_MILLIS)
            return delegate.viewListing(listingID)
        }

        override suspend fun hasReviewForListing(listingId: MarketplaceItemId, reviewerId: String): Boolean {
            reviewChecks += 1
            delay(LOOKUP_MILLIS)
            return delegate.hasReviewForListing(listingId, reviewerId)
        }
    }

    private class SlowProfiles : MarketplaceSellerProfileProvider {
        val requestedSellerIds = mutableListOf<String>()

        override suspend fun getSellerProfile(sellerId: String): MarketplaceSellerProfile? {
            requestedSellerIds += sellerId
            delay(LOOKUP_MILLIS)
            return MarketplaceSellerProfile(sellerId = sellerId, name = "Resolved Seller", school = null)
        }
    }

    private companion object {
        const val SELLER_ID = "seller_101"
        const val LOOKUP_MILLIS = 100L
    }
}
