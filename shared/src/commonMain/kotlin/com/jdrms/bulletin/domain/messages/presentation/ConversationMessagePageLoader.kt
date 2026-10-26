package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.GetConversationMessages
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.service.MessageTimeline
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
        uiState.update { it.copy(isLoadingMessages = true) }
        val result = getConversationMessages(conversationId)
        if (uiState.value.selectedConversationId != conversationId) return
        when (result) {
            is Result.Error -> showFailure(result)
            is Result.Success -> {
                if (inbox.state.value.conversations.none { it.id == conversationId }) {
                    uiState.update { it.clearMessageLoadingForMissingConversation(conversationId) }
                    return
                }
                when (val reconciled = inbox.reconcileMessages(conversationId, result.data.messages)) {
                    is Result.Error -> showFailure(reconciled)
                    is Result.Success -> uiState.update { state ->
                        state.copy(
                            currentMessages = MessageTimeline.merge(state.currentMessages, result.data.messages),
                            olderCursor = if (initialPageLoaded) state.olderCursor else result.data.olderCursor,
                            isLoadingMessages = false,
                            isLoadingOlder = false
                        )
                    }
                }
                readReceiptHandler.markIfNeeded(conversationId)
                initialPageLoaded = true
            }
        }
    }
}
