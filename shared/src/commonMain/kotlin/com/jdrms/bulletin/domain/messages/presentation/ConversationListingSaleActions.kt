package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.ConversationListingPreview
import com.jdrms.bulletin.domain.messages.application.ConversationListingSales
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Optimistically presents seller-side sold/undo changes while the backend confirms the operation. */
internal class ConversationListingSaleActions(
    private val state: MutableStateFlow<MessagesUiState>,
    private val sales: ConversationListingSales?,
    private val refreshPreview: (String) -> Unit,
    private val scope: CoroutineScope
) {
    fun markSold(listingId: String, buyerId: String) {
        val port = sales ?: return
        run(listingId, { it.copy(isActive = false, isSold = true, soldToBuyerId = buyerId) }) {
            port.markSold(ListingReferenceId(listingId), buyerId)
        }
    }

    fun undoSale(listingId: String) {
        val port = sales ?: return
        run(listingId, { it.copy(isActive = true, isSold = false, soldToBuyerId = null) }) {
            port.undoSale(ListingReferenceId(listingId))
        }
    }

    private fun run(
        listingId: String,
        optimisticPreview: (ConversationListingPreview) -> ConversationListingPreview,
        operation: suspend () -> Result<Unit>
    ) {
        val pending = claim(listingId, optimisticPreview) ?: return
        scope.launch {
            val result = runCatching { operation() }.getOrElse { exception ->
                if (exception is CancellationException) throw exception
                Result.Error(exception)
            }
            // A failure can be partial (restored but review cleanup failed), so reconcile after rollback too.
            if (finish(pending, result)) refreshPreview(listingId)
        }.invokeOnCompletion { cause ->
            if (cause is CancellationException) finish(pending, null)
        }
    }

    private fun claim(
        listingId: String,
        optimisticPreview: (ConversationListingPreview) -> ConversationListingPreview
    ): ConversationListingSaleUpdate? {
        while (true) {
            val current = state.value
            if (current.isUpdatingListingSale) return null
            val previous = current.listingPreviews[listingId] ?: return null
            val pending = ConversationListingSaleUpdate(
                listingId,
                previous,
                optimisticPreview(previous),
                current.selectedConversationId
            )
            val claimed = current.copy(
                listingSaleUpdate = pending,
                listingPreviews = current.listingPreviews + (listingId to pending.optimisticPreview),
                listingSaleError = null,
                statusMessage = null
            )
            if (state.compareAndSet(current, claimed)) return pending
        }
    }

    private fun finish(pending: ConversationListingSaleUpdate, result: Result<Unit>?): Boolean {
        while (true) {
            val current = state.value
            // Signing out or changing accounts replaces the thread state; old results must not affect it.
            if (current.listingSaleUpdate !== pending) return false
            val preview = if (result is Result.Success) pending.optimisticPreview else pending.previousPreview
            val previews = if (pending.listingId in current.listingPreviews) {
                current.listingPreviews + (pending.listingId to preview)
            } else {
                current.listingPreviews
            }
            val error = (result as? Result.Error)?.message?.ifBlank { FALLBACK_ERROR }
            val finished = current.copy(
                listingSaleUpdate = null,
                listingPreviews = previews,
                listingSaleError = error.takeIf { current.selectedConversationId == pending.conversationId }
            )
            if (state.compareAndSet(current, finished)) return true
        }
    }

    private companion object {
        const val FALLBACK_ERROR = "Couldn't update this listing. Try again."
    }
}
