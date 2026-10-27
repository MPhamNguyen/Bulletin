package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.application.ContactSellerFailure
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceListingSnapshot
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class HomeContactVisibilityIntegrationTest {
    @Test
    fun findsTheResolvedListingBeyondTheFirstVisibleSellerPage() = runTest {
        val f = HomeContactFixture().apply {
            additionalListings = (1..45).map { index ->
                MarketplaceListingSnapshot(
                    "newer-$index", listing.sellerId, listing.sellerName, "Newer listing", "Description",
                    10.0, "USD", MarketplaceCategory.TEXTBOOKS, index.toLong()
                )
            }
        }
        assertIs<Result.Success<Unit>>(f.adapter.send("viewer", actionListingId, actionMessage))
        assertEquals(3, f.visibilityRequests.size)
        assertNotNull(f.visibilityRequests[1].cursor)
        assertEquals(setOf(f.listing.sellerId), f.visibilityRequests.map { it.sellerId }.toSet())
        assertEquals(1, f.sent.size)
    }

    @Test
    fun rechecksVisibilityOnEveryAttemptAfterSellerOrListingIsHidden() = runTest {
        val f = HomeContactFixture()
        assertIs<Result.Success<Unit>>(f.adapter.send("viewer", actionListingId, actionMessage))
        f.hidden = setOf(f.listing.sellerId)
        assertIs<ContactSellerFailure.ListingUnavailable>(
            assertIs<Result.Error>(f.adapter.send("viewer", actionListingId, actionMessage)).exception
        )
        f.hidden = emptySet()
        f.reported = setOf(f.listing.id)
        assertIs<ContactSellerFailure.ListingUnavailable>(
            assertIs<Result.Error>(f.adapter.send("viewer", actionListingId, actionMessage)).exception
        )
        f.reported = emptySet()
        f.available = false
        assertIs<ContactSellerFailure.ListingUnavailable>(
            assertIs<Result.Error>(f.adapter.send("viewer", actionListingId, actionMessage)).exception
        )
        assertEquals(1, f.sent.size)
        assertEquals(1, f.senderCalls)
    }

    @Test
    fun visibilityFailuresMapSafelyAndCancellationPropagatesBeforeSending() = runTest {
        val f = HomeContactFixture().apply { visibilityFailure = IOException("private-seller-id") }
        val network = assertIs<Result.Error>(f.adapter.send("viewer", actionListingId, actionMessage)).exception
        assertIs<ContactSellerFailure.Network>(network)
        assertNull(network.message)
        assertNull(network.cause)
        f.visibilityFailure = IllegalStateException("Private Seller")
        assertIs<ContactSellerFailure.Unknown>(
            assertIs<Result.Error>(f.adapter.send("viewer", actionListingId, actionMessage)).exception
        )
        f.visibilityFailure = CancellationException()
        assertFailsWith<CancellationException> { f.adapter.send("viewer", actionListingId, actionMessage) }
        assertEquals(0, f.senderCalls)
        assertEquals(emptyList(), f.sent)
    }

    @Test
    fun lookupFailuresAreTranslatedBeforeAnyVisibilityOrMessagingCall() = runTest {
        val f = HomeContactFixture().apply { lookupFailure = IOException("private-seller-id") }
        assertIs<ContactSellerFailure.Network>(
            assertIs<Result.Error>(f.adapter.send("viewer", actionListingId, actionMessage)).exception
        )
        f.lookupThrows = IllegalStateException("Private Seller")
        val unknown = assertIs<Result.Error>(f.adapter.send("viewer", actionListingId, actionMessage)).exception
        assertIs<ContactSellerFailure.Unknown>(unknown)
        assertNull(unknown.cause)
        assertEquals(emptyList(), f.visibilityRequests)
        assertEquals(0, f.senderCalls)
    }
}
