package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.Message

/** Detach cached authors when a refreshed membership snapshot confirms their removal. */
internal fun MessagesUiState.withRetainedHistory(conversation: Conversation): MessagesUiState {
    if (selectedConversationId != conversation.id) return this
    val history = currentMessages.map { message ->
        if (message.senderId != null && !conversation.includes(message.senderId)) {
            message.copy(senderId = null, senderName = Message.MISSING_SENDER_NAME)
        } else {
            message
        }
    }
    val readOnly = conversation.isReadOnly || history.any { it.senderId == null }
    return copy(
        currentMessages = history,
        editState = if (readOnly) MessageEditState.Composing else editState,
        messageInput = if (readOnly) unsentInput else messageInput,
        editError = if (readOnly) null else editError
    )
}
