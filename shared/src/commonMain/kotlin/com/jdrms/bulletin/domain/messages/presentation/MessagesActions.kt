package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.ReportMessage
import com.jdrms.bulletin.domain.messages.application.SendMessage
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.service.MessageTimeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class MessagesActions(
    private val inbox: InboxStateOwner,
    private val state: MutableStateFlow<MessagesUiState>,
    private val sendMessage: SendMessage,
    private val reportMessage: ReportMessage,
    private val scope: CoroutineScope
) {
    fun send(showFailure: (Result.Error) -> Unit) {
        val snapshot = state.value
        val conversationId = snapshot.selectedConversationId ?: return
        if (snapshot.isSending || inbox.state.value.isLoading || snapshot.isLoadingMessages) return
        if (snapshot.messageInput.isBlank()) return
        state.update { it.copy(isSending = true, errorMessage = null, statusMessage = null) }
        scope.launch {
            when (val sent = sendMessage(conversationId, snapshot.messageInput)) {
                is Result.Error -> showFailure(sent)
                is Result.Success -> when (val recorded = inbox.recordMessage(sent.data)) {
                    is Result.Error -> showFailure(recorded)
                    is Result.Success -> state.update { current ->
                        current.copy(
                            currentMessages = MessageTimeline.merge(current.currentMessages, listOf(sent.data)),
                            messageInput = if (current.selectedConversationId == conversationId) {
                                ""
                            } else {
                                current.messageInput
                            },
                            isSending = false,
                            statusMessage = "Message sent."
                        )
                    }
                }
            }
        }
    }

    fun report(
        messageId: MessageId,
        reason: String,
        onReported: (ConversationId) -> Unit,
        showFailure: (Result.Error) -> Unit
    ) {
        val conversationId = state.value.selectedConversationId ?: return
        if (state.value.isReporting || inbox.state.value.isLoading) return
        state.update { it.copy(isReporting = true, errorMessage = null, statusMessage = null) }
        scope.launch {
            when (val result = reportMessage(conversationId, messageId, reason)) {
                is Result.Error -> showFailure(result)
                is Result.Success -> {
                    if (state.value.selectedConversationId == conversationId) {
                        onReported(conversationId)
                        state.update { it.copy(statusMessage = "Message reported.") }
                    }
                    state.update { it.copy(isReporting = false) }
                }
            }
        }
    }
}
