package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationListingSaleOptimismTest {
    @Test
    fun undoImmediatelyShowsAvailableAndKeepsOtherPreviewDetails() = runTest {
        val previous = listingSalePreview(sold = true)
        val state = MutableStateFlow(MessagesUiState(listingPreviews = mapOf(SALE_LISTING_ID to previous)))
        val sales = DeferredListingSales()
        val refreshed = mutableListOf<String>()
        val actions = ConversationListingSaleActions(state, sales, { refreshed += it }, backgroundScope)
        actions.undoSale(SALE_LISTING_ID)
        assertEquals(
            previous.copy(isActive = true, isSold = false, soldToBuyerId = null),
            state.value.listingPreviews[SALE_LISTING_ID]
        )
        assertTrue(state.value.isUpdatingListingSale)
        actions.markSold(SALE_LISTING_ID, "buyer")
        sales.result.complete(Result.Success(Unit))
        runCurrent()
        assertEquals(listOf("undo:$SALE_LISTING_ID"), sales.calls)
        assertFalse(state.value.isUpdatingListingSale)
        assertEquals(listOf(SALE_LISTING_ID), refreshed)
        assertNull(state.value.statusMessage)
    }

    @Test
    fun failedSaleRestoresThePreviousCardAndShowsTheError() = runTest {
        val previous = listingSalePreview()
        val state = MutableStateFlow(MessagesUiState(listingPreviews = mapOf(SALE_LISTING_ID to previous)))
        val sales = DeferredListingSales()
        val actions = ConversationListingSaleActions(state, sales, {}, backgroundScope)
        actions.markSold(SALE_LISTING_ID, "buyer")
        assertEquals(
            previous.copy(isActive = false, isSold = true, soldToBuyerId = "buyer"),
            state.value.listingPreviews[SALE_LISTING_ID]
        )
        sales.result.complete(Result.Error(IllegalStateException("Offline")))
        runCurrent()
        assertEquals(previous, state.value.listingPreviews[SALE_LISTING_ID])
        assertEquals("Offline", state.value.listingSaleError)
        assertFalse(state.value.isUpdatingListingSale)
    }

    @Test
    fun oldAccountCompletionCannotOverwriteNewAccountsPendingSale() = runTest {
        val state = MutableStateFlow(MessagesUiState(listingPreviews = mapOf(SALE_LISTING_ID to listingSalePreview())))
        val oldSales = DeferredListingSales()
        val newSales = DeferredListingSales()
        val refreshed = mutableListOf<String>()
        ConversationListingSaleActions(state, oldSales, { refreshed += it }, backgroundScope)
            .markSold(SALE_LISTING_ID, "old-buyer")
        runCurrent()
        // MessagesViewModel replaces thread state when the signed-in identity changes.
        state.value = MessagesUiState(listingPreviews = mapOf(SALE_LISTING_ID to listingSalePreview()))
        ConversationListingSaleActions(state, newSales, { refreshed += it }, backgroundScope)
            .markSold(SALE_LISTING_ID, "new-buyer")
        val newAccount = state.value
        oldSales.result.complete(Result.Error(IllegalStateException("Old account failed")))
        runCurrent()
        assertEquals(newAccount, state.value)
        assertEquals(emptyList(), refreshed)
        newSales.result.complete(Result.Success(Unit))
        runCurrent()
        assertEquals("new-buyer", state.value.listingPreviews[SALE_LISTING_ID]?.soldToBuyerId)
        assertFalse(state.value.isUpdatingListingSale)
        assertNull(state.value.listingSaleError)
    }

    @Test
    fun cancellationBeforeDispatchRestoresTheCardAndUnlocksTheAction() = runTest {
        val previous = listingSalePreview()
        val state = MutableStateFlow(MessagesUiState(listingPreviews = mapOf(SALE_LISTING_ID to previous)))
        ConversationListingSaleActions(state, DeferredListingSales(), {}, backgroundScope)
            .markSold(SALE_LISTING_ID, "buyer")
        backgroundScope.coroutineContext.cancelChildren()
        runCurrent()
        assertEquals(previous, state.value.listingPreviews[SALE_LISTING_ID])
        assertFalse(state.value.isUpdatingListingSale)
        assertNull(state.value.listingSaleError)
    }

    @Test
    fun cancellationWhileSavingAlsoRollsBackWithoutShowingAnError() = runTest {
        val previous = listingSalePreview(sold = true)
        val state = MutableStateFlow(MessagesUiState(listingPreviews = mapOf(SALE_LISTING_ID to previous)))
        val sales = DeferredListingSales()
        ConversationListingSaleActions(state, sales, {}, backgroundScope).undoSale(SALE_LISTING_ID)
        runCurrent()
        assertEquals(listOf("undo:$SALE_LISTING_ID"), sales.calls)
        backgroundScope.coroutineContext.cancelChildren()
        runCurrent()
        assertEquals(previous, state.value.listingPreviews[SALE_LISTING_ID])
        assertFalse(state.value.isUpdatingListingSale)
        assertNull(state.value.listingSaleError)
    }

    @Test
    fun failureInAnotherThreadDoesNotShowAnErrorOnTheSelectedConversation() = runTest {
        val previous = listingSalePreview()
        val state = MutableStateFlow(
            MessagesUiState(
                selectedConversationId = ConversationId("a"),
                listingPreviews = mapOf(SALE_LISTING_ID to previous)
            )
        )
        val sales = DeferredListingSales()
        ConversationListingSaleActions(state, sales, {}, backgroundScope).markSold(SALE_LISTING_ID, "buyer")
        state.value = state.value.copy(selectedConversationId = ConversationId("b"))
        sales.result.complete(Result.Error(IllegalStateException("Offline")))
        runCurrent()
        assertEquals(previous, state.value.listingPreviews[SALE_LISTING_ID])
        assertNull(state.value.listingSaleError)
    }
}
