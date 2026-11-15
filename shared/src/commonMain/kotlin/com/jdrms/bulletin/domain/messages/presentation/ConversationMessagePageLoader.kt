package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.GetConversationMessages
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.MessageCursor
import com.jdrms.bulletin.domain.messages.domain.model.MessagePage
import com.jdrms.bulletin.domain.messages.domain.service.MessageTimeline
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Owns initial history loading and reconciles the page with the live inbox snapshot. */
internal class ConversationMessagePageLoader(
    private val uiState: MutableStateFlow<MessagesUiState>,
    private val inbox: InboxStateOwner,
    private val getConversationMessages: GetConversationMessages,
    private val readReceiptHandler: ConversationReadReceiptHandler,
    private val showFailure: (Result.Error) -> Unit
) {
    var initialPageLoaded: Boolean = false
        private set

    fun reset() {
        initialPageLoaded = false
    }

    suspend fun load(conversationId: ConversationId) {
        val oldestLoaded = uiState.value.currentMessages.firstOrNull()
        val through = oldestLoaded?.let { MessageCursor(it.orderingTimestamp, it.id) }
        load(MessageHistoryRequest.Thread(conversationId, through))
    }

    suspend fun load(request: MessageHistoryRequest.Thread) {
        uiState.update { it.copy(isLoadingMessages = true) }
        val result = getConversationMessages(request.conversationId, through = request.through)
        currentCoroutineContext().ensureActive()
        if (uiState.value.selectedConversationId != request.conversationId) return
        when (result) {
            is Result.Error -> fail(request, result)
            is Result.Success -> acceptPage(request, result.data)
        }
    }

    private fun acceptPage(request: MessageHistoryRequest.Thread, page: MessagePage) {
        val conversationId = request.conversationId
        if (inbox.state.value.conversations.none { it.id == conversationId }) {
            uiState.update { it.clearMessageLoadingForMissingConversation(conversationId) }
            return
        }
        when (val reconciled = inbox.reconcileMessages(conversationId, page.messages)) {
            is Result.Error -> fail(request, reconciled)
            is Result.Success -> {
                uiState.update { state ->
                    state.copy(
                        currentMessages = MessageTimeline.merge(state.currentMessages, page.messages),
                        olderCursor = if (initialPageLoaded) state.olderCursor else page.olderCursor,
                        isLoadingMessages = false,
                        failedHistoryRequest = null
                    )
                }
                initialPageLoaded = true
                readReceiptHandler.markIfNeeded(conversationId)
            }
        }
    }

    private fun fail(request: MessageHistoryRequest.Thread, error: Result.Error) {
        if (error.isMessagingIdentityUnavailable) {
            showFailure(error)
        } else {
            uiState.update { it.copy(isLoadingMessages = false, failedHistoryRequest = request) }
        }
    }
}
