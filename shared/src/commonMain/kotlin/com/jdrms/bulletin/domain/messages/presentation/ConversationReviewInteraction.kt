package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.SubmitConversationReview
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class ConversationReviewInteraction(
    private val submitReview: SubmitConversationReview?,
    private val currentState: () -> MessagesUiState,
    private val scope: CoroutineScope
) {
    private val mutableState = MutableStateFlow<Map<String, ConversationReviewState>>(emptyMap())
    val state = mutableState.asStateFlow()
    private var generation = 0

    fun reset() {
        generation++
        mutableState.value = emptyMap()
    }

    fun submit(rating: Int, comment: String) {
        val snapshot = currentState()
        val buyer = snapshot.viewerId ?: return
        val conversation = snapshot.conversations.firstOrNull { it.id == snapshot.selectedConversationId } ?: return
        val listing = conversation.listingId
        val seller = conversation.counterpart(buyer)
        if (listing == null || seller == null ||
            buyerRatingPromptSeller(snapshot.listingPreviews[listing.value], buyer, seller) == null
        ) {
            return
        }
        val previous = state.value[listing.value]
        if (previous == ConversationReviewState.Submitting || previous == ConversationReviewState.Submitted) return
        val version = generation
        mutableState.update { it + (listing.value to ConversationReviewState.Submitting) }
        scope.launch {
            val result = save { submitReview?.invoke(listing, buyer, seller.id, rating, comment) }
            if (version == generation) mutableState.update { it + (listing.value to result) }
        }
    }

    private suspend fun save(action: suspend () -> Result<Unit>?): ConversationReviewState = try {
        when (val result = action()) {
            is Result.Success -> ConversationReviewState.Submitted
            is Result.Error -> ConversationReviewState.Failed(result.exception.message ?: SAVE_FAILURE)
            null -> ConversationReviewState.Failed(SAVE_FAILURE)
        }
    } catch (cancelled: CancellationException) {
        throw cancelled
    } catch (_: Exception) {
        ConversationReviewState.Failed(SAVE_FAILURE)
    }

    private companion object {
        const val SAVE_FAILURE = "Couldn't submit your review. Try again."
    }
}
