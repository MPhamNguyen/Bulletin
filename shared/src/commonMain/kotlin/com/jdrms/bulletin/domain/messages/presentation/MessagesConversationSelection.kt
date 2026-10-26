package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class MessagesConversationSelection(
    private val state: MutableStateFlow<MessagesUiState>,
    private val cancelCurrentLoads: () -> Unit,
    private val resetPage: () -> Unit,
    private val isDestinationActive: () -> Boolean,
    private val reload: (ConversationId) -> Unit,
    private val observe: (ConversationId) -> Unit
) {
    fun select(conversationId: ConversationId?) {
        cancelCurrentLoads()
        resetPage()
        state.update { current ->
            current.copy(
                selectedConversationId = conversationId,
                currentMessages = emptyList(),
                messageReportForm = if (current.selectedConversationId == conversationId) {
                    current.messageReportForm
                } else {
                    null
                },
                revealedReportedMessageIds = emptySet(),
                olderCursor = null,
                isLoadingMessages = conversationId != null,
                isLoadingOlder = false,
                errorMessage = null,
                statusMessage = null
            )
        }
        if (isDestinationActive() && conversationId != null) {
            reload(conversationId)
            observe(conversationId)
        }
    }
}
