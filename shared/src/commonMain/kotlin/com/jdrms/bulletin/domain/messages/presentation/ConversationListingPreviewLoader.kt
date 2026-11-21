package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.application.ConversationListingPreviewProvider
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Loads only the listing previews needed by the current inbox and cancels obsolete requests. */
internal class ConversationListingPreviewLoader(
    private val uiState: MutableStateFlow<MessagesUiState>,
    private val provider: ConversationListingPreviewProvider,
    private val scope: CoroutineScope
) {
    private val jobs = mutableMapOf<String, Job>()

    fun load(inbox: InboxState) = load(inbox.conversations, inbox.unmatchedReviewRequests.map { it.listingId })

    fun load(conversations: List<Conversation>, reviewListings: List<ListingReferenceId> = emptyList()) {
        val listingIds = conversations.mapNotNull { it.listingId?.value }.toSet() + reviewListings.map { it.value }
        jobs.keys.filterNot(listingIds::contains).forEach { listingId -> jobs.remove(listingId)?.cancel() }
        uiState.update { state -> state.copy(listingPreviews = state.listingPreviews.filterKeys(listingIds::contains)) }
        val loadedIds = uiState.value.listingPreviews.keys
        (listingIds - loadedIds - jobs.keys).forEach(::load)
    }

    /** Reloads one preview, e.g. after its listing was sold or restored. */
    fun refresh(listingId: String) {
        jobs.remove(listingId)?.cancel()
        load(listingId)
    }

    fun cancel() {
        jobs.values.forEach { it.cancel() }
        jobs.clear()
    }

    private fun load(listingId: String) {
        val job = scope.launch(start = CoroutineStart.LAZY) {
            val preview = try {
                provider.preview(ListingReferenceId(listingId))
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (_: Exception) {
                // Keep the last known card if a background refresh is offline.
                return@launch
            }
            currentCoroutineContext().ensureActive()
            uiState.update { state ->
                if (state.listingSaleUpdate?.listingId == listingId) {
                    state
                } else {
                    state.copy(listingPreviews = state.listingPreviews + (listingId to preview))
                }
            }
        }
        jobs[listingId] = job
        job.invokeOnCompletion { if (jobs[listingId] === job) jobs.remove(listingId) }
        job.start()
    }
}
