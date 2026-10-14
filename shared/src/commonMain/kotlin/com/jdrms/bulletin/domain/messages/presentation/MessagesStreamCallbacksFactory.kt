package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.service.MessageTimeline
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class MessagesStreamCallbacksFactory(
    private val uiState: MutableStateFlow<MessagesUiState>,
    private val reloadSelectedConversation: (ConversationId) -> Unit,
    private val showFailure: (Result.Error) -> Unit,
    private val shouldReloadAfterFailure: () -> Boolean
) {
    fun forConversation(conversationId: ConversationId) = MessageStreamCallbacks(
        isCurrent = { uiState.value.selectedConversationId == conversationId },
        onConnected = {
            uiState.update { it.copy(connectionState = MessageConnectionState.CONNECTED) }
            reloadSelectedConversation(conversationId)
        },
        onConnectionState = { connectionState ->
            uiState.update { it.copy(connectionState = connectionState) }
        },
        onReceived = { message ->
            val senderName = uiState.value.conversations
                .firstOrNull { it.id == conversationId }
                ?.participants
                ?.firstOrNull { it.id == message.senderId }
                ?.displayName
            val resolvedMessage = senderName?.let { message.copy(senderName = it) } ?: message
            uiState.update {
                it.copy(currentMessages = MessageTimeline.merge(it.currentMessages, listOf(resolvedMessage)))
            }
        },
        onFailed = { cause, terminal ->
            if (terminal) {
                showFailure(Result.Error(cause))
            } else {
                uiState.update { it.copy(connectionState = MessageConnectionState.DISCONNECTED) }
                if (shouldReloadAfterFailure()) reloadSelectedConversation(conversationId)
            }
        }
    )
}
