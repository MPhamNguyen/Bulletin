package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.ConversationListingPreview
import com.jdrms.bulletin.domain.messages.application.ConversationListingSales
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationListingSaleActionsTest {
    @Test
    fun saleDisablesRepeatedSubmissionsAndRefreshesPreview() = runTest {
        val state = saleState()
        val pending = CompletableDeferred<Result<Unit>>()
        val calls = mutableListOf<Pair<String, String>>()
        val refreshed = mutableListOf<String>()
        val port = object : ConversationListingSales {
            override suspend fun markSold(listingId: ListingReferenceId, buyerId: String): Result<Unit> {
                calls += listingId.value to buyerId
                return pending.await()
            }
            override suspend fun undoSale(listingId: ListingReferenceId): Result<Unit> = error("Unexpected undo")
        }
        val actions = ConversationListingSaleActions(state, port, { refreshed += it }, this)
        actions.markSold("listing:1", "buyer")
        actions.markSold("listing:1", "buyer")
        assertTrue(state.value.listingPreviews["listing:1"]?.isSold == true)
        assertEquals("buyer", state.value.listingPreviews["listing:1"]?.soldToBuyerId)
        runCurrent()
        assertTrue(state.value.isUpdatingListingSale)
        assertEquals(listOf("listing:1" to "buyer"), calls)
        pending.complete(Result.Success(Unit))
        runCurrent()
        assertFalse(state.value.isUpdatingListingSale)
        assertEquals(listOf("listing:1"), refreshed)
        assertNull(state.value.statusMessage)
        assertNull(state.value.listingSaleError)
    }

    @Test
    fun failedUndoRefreshesPartialSuccessAndAllowsRetry() = runTest {
        val state = saleState()
        val refreshed = mutableListOf<String>()
        var undoCalls = 0
        val port = object : ConversationListingSales {
            override suspend fun markSold(listingId: ListingReferenceId, buyerId: String): Result<Unit> =
                error("Unexpected sale")
            override suspend fun undoSale(listingId: ListingReferenceId): Result<Unit> {
                assertEquals("listing:1", listingId.value)
                undoCalls++
                return if (undoCalls == 1) {
                    Result.Error(IllegalStateException("Cleanup failed"))
                } else {
                    Result.Success(Unit)
                }
            }
        }
        val actions = ConversationListingSaleActions(state, port, { refreshed += it }, this)
        actions.undoSale("listing:1")
        runCurrent()
        assertEquals("Cleanup failed", state.value.listingSaleError)
        assertNull(state.value.statusMessage)
        assertFalse(state.value.isUpdatingListingSale)
        actions.undoSale("listing:1")
        runCurrent()
        assertEquals(2, undoCalls)
        assertEquals(listOf("listing:1", "listing:1"), refreshed)
        assertNull(state.value.listingSaleError)
        assertNull(state.value.statusMessage)
    }

    @Test
    fun thrownSaleFailureIsPresentedAndRefreshesPreview() = runTest {
        val state = saleState()
        val refreshed = mutableListOf<String>()
        val port = object : ConversationListingSales {
            override suspend fun markSold(listingId: ListingReferenceId, buyerId: String): Result<Unit> =
                error("Offline")
            override suspend fun undoSale(listingId: ListingReferenceId): Result<Unit> = error("Unexpected undo")
        }
        ConversationListingSaleActions(state, port, { refreshed += it }, this)
            .markSold("listing:1", "buyer")
        runCurrent()
        assertEquals("Offline", state.value.listingSaleError)
        assertFalse(state.value.isUpdatingListingSale)
        assertNull(state.value.statusMessage)
        assertEquals(listOf("listing:1"), refreshed)
    }

    private fun saleState() = MutableStateFlow(
        MessagesUiState(listingPreviews = mapOf("listing:1" to ConversationListingPreview("Desk", "$10", null)))
    )
}
