package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.application.ContactSellerFailure
import com.jdrms.bulletin.domain.messages.application.MessageSenderLookupException
import com.jdrms.bulletin.domain.messages.application.MessagingAuthenticationRequiredException
import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import com.jdrms.bulletin.domain.messages.infrastructure.repository.MessagesPersistenceException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class MessagesHomeContactSellerAdapterTest {
    @Test
    fun successResolvesSellerAndSendsTheValidatedTextExactlyOnce() = runTest {
        val f = HomeContactFixture()
        assertIs<Result.Success<Unit>>(f.adapter.send("viewer", actionListingId, actionMessage))
        assertEquals(listOf(actionListingId.value), f.lookupIds)
        assertEquals(actionMessage.text, f.sent.single().content)
        assertEquals("original", f.resolvedListingId?.value)
        assertEquals(f.listing.sellerId, f.resolvedSeller?.id?.value)
        assertEquals(f.listing.sellerName, f.resolvedSeller?.displayName)
        assertEquals("viewer", f.visibilityRequests.single().viewerId)
        assertEquals(actionListingId.value, f.visibilityRequests.single().listingId)
        assertEquals(1, f.visibilityRequests.single().pageSize)
    }

    @Test
    fun hiddenReportedSoldUnavailableAndMissingListingsCannotBeContacted() = runTest {
        val cases: List<HomeContactFixture.() -> Unit> = listOf(
            { hidden = setOf(listing.sellerId) },
            { reported = setOf(listing.id) },
            { listing = listing.copy(isSold = true) },
            { listing = listing.copy(status = "UNAVAILABLE") },
            { available = false },
            { lookupFailure = NoSuchElementException("private-seller-id") },
            { lookupFailure = IllegalStateException("Listing not found.") }
        )
        cases.forEach { arrange ->
            val f = HomeContactFixture().apply(arrange)
            val failure = assertIs<Result.Error>(f.adapter.send("viewer", actionListingId, actionMessage)).exception
            assertIs<ContactSellerFailure.ListingUnavailable>(failure)
            assertNull(failure.message)
            assertNull(failure.cause)
            assertEquals(emptyList(), f.sent)
            assertEquals(0, f.senderCalls)
        }
    }

    @Test
    fun ownListingIsRejectedBeforeCallingMessageSellerOrVisibility() = runTest {
        val f = HomeContactFixture().apply { listing = listing.copy(sellerId = "viewer") }
        val failure = assertIs<Result.Error>(f.adapter.send("viewer", actionListingId, actionMessage)).exception
        assertIs<ContactSellerFailure.OwnListing>(failure)
        assertEquals(emptyList(), f.sent)
        assertEquals(emptyList(), f.visibilityRequests)
        assertEquals(0, f.senderCalls)
    }

    @Test
    fun mapsEveryMessagingFailureWithoutExposingSellerIdentityOrRawCauses() = runTest {
        val cases = listOf(
            MessagingAuthenticationRequiredException() to ContactSellerFailure.NotSignedIn::class,
            ConversationAccessException() to ContactSellerFailure.ListingUnavailable::class,
            IllegalArgumentException("You cannot message yourself.") to ContactSellerFailure.OwnListing::class,
            IOException("private-seller-id") to ContactSellerFailure.Network::class,
            MessagesPersistenceException(IOException("Private Seller")) to ContactSellerFailure.Network::class,
            MessageSenderLookupException(IOException("private-seller-id")) to ContactSellerFailure.Network::class,
            IllegalArgumentException("Message content cannot be blank.") to ContactSellerFailure.Unknown::class,
            IllegalArgumentException("Message cannot exceed 1000 characters.") to ContactSellerFailure.Unknown::class,
            IllegalStateException("private-seller-id Private Seller") to ContactSellerFailure.Unknown::class
        )
        cases.forEach { (error, expected) ->
            val f = HomeContactFixture().apply { senderFailure = error }
            val failure = assertIs<Result.Error>(f.adapter.send("viewer", actionListingId, actionMessage)).exception
            assertEquals(expected, failure::class)
            assertNull(failure.message)
            assertNull(failure.cause)
        }
    }

    @Test
    fun repositorySendErrorsAndThrownErrorsAreTranslatedWithoutRetry() = runTest {
        val f = HomeContactFixture().apply { sendFailure = MessagesPersistenceException(IOException("offline")) }
        assertIs<ContactSellerFailure.Network>(
            assertIs<Result.Error>(f.adapter.send("viewer", actionListingId, actionMessage)).exception
        )
        assertEquals(1, f.sent.size)
        f.sendThrows = IllegalStateException("private-seller-id")
        assertIs<ContactSellerFailure.Unknown>(
            assertIs<Result.Error>(f.adapter.send("viewer", actionListingId, actionMessage)).exception
        )
        assertEquals(2, f.sent.size)
    }

    @Test
    fun cancellationPropagatesFromLookupAndSendIncludingResultErrors() = runTest {
        val cases: List<HomeContactFixture.() -> Unit> = listOf(
            { lookupThrows = CancellationException() },
            { lookupFailure = CancellationException() },
            { sendThrows = CancellationException() },
            { sendFailure = CancellationException() }
        )
        cases.forEach { arrange ->
            val f = HomeContactFixture().apply(arrange)
            assertFailsWith<CancellationException> { f.adapter.send("viewer", actionListingId, actionMessage) }
        }
    }

    @Test
    fun blankViewerIsRejectedBeforeAnyContextCall() = runTest {
        val f = HomeContactFixture()
        assertIs<ContactSellerFailure.NotSignedIn>(
            assertIs<Result.Error>(f.adapter.send(" ", actionListingId, actionMessage)).exception
        )
        assertEquals(emptyList(), f.lookupIds)
    }
}
