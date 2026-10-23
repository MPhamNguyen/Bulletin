package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.MarkConversationRead
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.time.Instant

/** Advances the signed-in participant's read watermark while their conversation is open. */
internal class ConversationReadReceiptHandler(
    private val inbox: InboxStateOwner,
    private val uiState: MutableStateFlow<MessagesUiState>,
    private val viewerId: () -> SenderId?,
    private val markConversationRead: MarkConversationRead?,
    private val scope: CoroutineScope,
    private val showFailure: (Result.Error) -> Unit
) {
    private val readJobs = mutableMapOf<ConversationId, Job>()

    fun markIfNeeded(conversationId: ConversationId) = markIfNeeded(conversationId, forceRemoteUpdate = false)

    private fun markIfNeeded(conversationId: ConversationId, forceRemoteUpdate: Boolean) {
        val request = readRequestOrNull(conversationId, forceRemoteUpdate) ?: return
        when (val localReceipt = inbox.recordReadAt(conversationId, request.readerId, request.readAt)) {
            is Result.Error -> {
                showFailure(localReceipt)
                return
            }
            is Result.Success -> Unit
        }
        val markRead = markConversationRead ?: return
        if (readJobs[conversationId]?.isActive == true) return
        persistReadReceipt(conversationId, request, markRead)
    }

    private fun persistReadReceipt(
        conversationId: ConversationId,
        request: ReadReceiptRequest,
        markRead: MarkConversationRead
    ) {
        var markedSuccessfully = false
        var serverReadAt: Instant? = null
        val job = scope.launch(start = CoroutineStart.LAZY) {
            when (val result = markRead(conversationId)) {
                is Result.Error -> showFailure(result)
                is Result.Success -> {
                    inbox.recordReadAt(conversationId, request.readerId, result.data)
                    markedSuccessfully = true
                    serverReadAt = result.data
                }
            }
        }
        readJobs[conversationId] = job
        job.invokeOnCompletion {
            if (readJobs[conversationId] === job) {
                readJobs.remove(conversationId)
                val latestIncomingAt = uiState.value.currentMessages
                    .lastOrNull { it.senderId != request.readerId }
                    ?.orderingTimestamp
                val needsFollowUp = serverReadAt?.let { persistedAt ->
                    latestIncomingAt?.let { it > persistedAt }
                } == true
                if (markedSuccessfully && needsFollowUp) {
                    markIfNeeded(conversationId, forceRemoteUpdate = true)
                }
            }
        }
        job.start()
    }

    private fun readRequestOrNull(
        conversationId: ConversationId,
        forceRemoteUpdate: Boolean
    ): ReadReceiptRequest? {
        val state = uiState.value
        val readerId = viewerId()?.takeIf {
            state.isConversationOpen && state.selectedConversationId == conversationId
        } ?: return null
        val latestIncoming = state.currentMessages.lastOrNull { it.senderId != readerId } ?: return null
        val conversation = inbox.state.value.conversations.firstOrNull { it.id == conversationId } ?: return null
        val readAt = conversation.participants.firstOrNull { it.id == readerId }?.lastReadAt
        return if (forceRemoteUpdate || readAt == null || latestIncoming.orderingTimestamp > readAt) {
            ReadReceiptRequest(readerId, latestIncoming.orderingTimestamp)
        } else {
            null
        }
    }

    private data class ReadReceiptRequest(val readerId: SenderId, val readAt: Instant)
}
