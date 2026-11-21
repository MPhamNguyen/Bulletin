package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.ConversationListingPreview
import com.jdrms.bulletin.domain.messages.application.ConversationListingPreviewProvider
import com.jdrms.bulletin.domain.messages.application.PendingConversationReview
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ConversationListingPreviewLoaderTest {
    @Test
    fun loadsPurchaseTitleForRequestsWithoutVisibleChatsAndDropsItWhenResolved() = runTest {
        val state = MutableStateFlow(MessagesUiState())
        val preview = listingSalePreview(sold = true)
        val loader = ConversationListingPreviewLoader(
            state,
            ConversationListingPreviewProvider { preview },
            backgroundScope
        )
        loader.load(
            InboxState(
                unmatchedReviewRequests = listOf(
                    PendingConversationReview(ListingReferenceId(SALE_LISTING_ID), SenderId("seller"))
                )
            )
        )
        runCurrent()
        assertEquals(preview, state.value.listingPreviews[SALE_LISTING_ID])
        loader.load(InboxState())
        assertTrue(state.value.listingPreviews.isEmpty())
    }

    @Test
    fun previewStartedBeforeTheTapCannotReplaceTheOptimisticSale() = runTest {
        val previous = listingSalePreview()
        val sold = listingSalePreview(sold = true)
        val stale = CompletableDeferred<ConversationListingPreview>()
        val state = MutableStateFlow(MessagesUiState(listingPreviews = mapOf(SALE_LISTING_ID to previous)))
        var reads = 0
        val provider = ConversationListingPreviewProvider { if (++reads == 1) stale.await() else sold }
        val loader = ConversationListingPreviewLoader(state, provider, backgroundScope)
        loader.refresh(SALE_LISTING_ID)
        runCurrent()
        val sales = DeferredListingSales()
        ConversationListingSaleActions(state, sales, loader::refresh, backgroundScope)
            .markSold(SALE_LISTING_ID, "buyer")
        stale.complete(previous)
        runCurrent()
        assertEquals(sold, state.value.listingPreviews[SALE_LISTING_ID])
        assertTrue(state.value.isUpdatingListingSale)
        sales.result.complete(Result.Success(Unit))
        runCurrent()
        assertEquals(2, reads)
        assertEquals(sold, state.value.listingPreviews[SALE_LISTING_ID])
        assertFalse(state.value.isUpdatingListingSale)
    }

    @Test
    fun offlineRefreshKeepsTheConfirmedOptimisticCard() = runTest {
        val state = MutableStateFlow(MessagesUiState(listingPreviews = mapOf(SALE_LISTING_ID to listingSalePreview())))
        val loader = ConversationListingPreviewLoader(
            state,
            ConversationListingPreviewProvider { error("Offline") },
            backgroundScope
        )
        val sales = DeferredListingSales()
        ConversationListingSaleActions(state, sales, loader::refresh, backgroundScope)
            .markSold(SALE_LISTING_ID, "buyer")
        sales.result.complete(Result.Success(Unit))
        runCurrent()
        assertEquals(listingSalePreview(sold = true), state.value.listingPreviews[SALE_LISTING_ID])
    }

    @Test
    fun failedUndoRefreshesTheActualStateAfterPartialSuccess() = runTest {
        val state = MutableStateFlow(
            MessagesUiState(listingPreviews = mapOf(SALE_LISTING_ID to listingSalePreview(sold = true)))
        )
        val loader = ConversationListingPreviewLoader(
            state,
            ConversationListingPreviewProvider { listingSalePreview() },
            backgroundScope
        )
        val sales = DeferredListingSales()
        ConversationListingSaleActions(state, sales, loader::refresh, backgroundScope).undoSale(SALE_LISTING_ID)
        sales.result.complete(Result.Error(IllegalStateException("Review cleanup failed")))
        runCurrent()
        assertEquals(listingSalePreview(), state.value.listingPreviews[SALE_LISTING_ID])
        assertEquals("Review cleanup failed", state.value.listingSaleError)
    }

    @Test
    fun cancelledPreviewCannotRepopulateStateAfterSessionReset() = runTest {
        val stale = CompletableDeferred<ConversationListingPreview>()
        val state = MutableStateFlow(MessagesUiState())
        val provider = ConversationListingPreviewProvider { withContext(NonCancellable) { stale.await() } }
        val loader = ConversationListingPreviewLoader(state, provider, backgroundScope)
        loader.refresh(SALE_LISTING_ID)
        runCurrent()
        loader.cancel()
        state.value = MessagesUiState()
        stale.complete(listingSalePreview())
        runCurrent()
        assertEquals(emptyMap(), state.value.listingPreviews)
    }
}
