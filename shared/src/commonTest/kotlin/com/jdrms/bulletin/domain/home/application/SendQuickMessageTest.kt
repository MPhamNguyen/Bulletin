package com.jdrms.bulletin.domain.home.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.SellerMessageFailure
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame

class SendQuickMessageTest {
    private val port = FakeContactSellerPort()
    private val send = SendQuickMessage(port)
    private val listingId = HomeItemId("listing")

    @Test
    fun successCallsPortExactlyOnceWithViewerListingAndTrimmedMessage() = runTest {
        assertIs<Result.Success<Unit>>(send("viewer", listingId, "  hello\nthere \t"))

        val call = port.calls.single()
        assertEquals("viewer", call.userId)
        assertEquals(listingId, call.listingId)
        assertEquals("hello\nthere", call.message.text)
    }

    @Test
    fun blankTextFailsValidationWithoutCallingPort() = runTest {
        listOf("", " \t\n").forEach { text ->
            val result = send("viewer", listingId, text)
            val failure = assertIs<SendQuickMessageFailure.Validation>(assertIs<Result.Error>(result).exception)
            assertIs<SellerMessageFailure.Blank>(failure.reason)
        }
        assertEquals(0, port.calls.size)
    }

    @Test
    fun tooLongTextFailsValidationWithoutCallingPort() = runTest {
        val result = send("viewer", listingId, "a".repeat(201))

        val failure = assertIs<SendQuickMessageFailure.Validation>(assertIs<Result.Error>(result).exception)
        assertIs<SellerMessageFailure.TooLong>(failure.reason)
        assertEquals(0, port.calls.size)
    }

    @Test
    fun missingViewerFailsWithoutCallingPort() = runTest {
        listOf(null, "", " \t").forEach { viewer ->
            val result = send(viewer, listingId, "hello")
            assertIs<SendQuickMessageFailure.NotSignedIn>(assertIs<Result.Error>(result).exception)
        }
        assertEquals(0, port.calls.size)
    }

    @Test
    fun mapsNotSignedIn() = runTest {
        assertIs<SendQuickMessageFailure.NotSignedIn>(failureFrom(ContactSellerFailure.NotSignedIn()))
    }

    @Test
    fun mapsListingUnavailable() = runTest {
        assertIs<SendQuickMessageFailure.ListingUnavailable>(failureFrom(ContactSellerFailure.ListingUnavailable()))
    }

    @Test
    fun mapsOwnListing() = runTest {
        assertIs<SendQuickMessageFailure.OwnListing>(failureFrom(ContactSellerFailure.OwnListing()))
    }

    @Test
    fun mapsNetworkAndPreservesCause() = runTest {
        val failure = ContactSellerFailure.Network()
        assertSame(failure, assertIs<SendQuickMessageFailure.Network>(failureFrom(failure)).cause)
    }

    @Test
    fun mapsUnknownAndPreservesCause() = runTest {
        val failure = ContactSellerFailure.Unknown()
        assertSame(failure, assertIs<SendQuickMessageFailure.Unknown>(failureFrom(failure)).cause)
    }

    private suspend fun failureFrom(failure: ContactSellerFailure): Throwable {
        port.respond = { Result.Error(failure) }
        val result = send("viewer", listingId, "hello")
        assertEquals(1, port.calls.size)
        return assertIs<Result.Error>(result).exception
    }
}
