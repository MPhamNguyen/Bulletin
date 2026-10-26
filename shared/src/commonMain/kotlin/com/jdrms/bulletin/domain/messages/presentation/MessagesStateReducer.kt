package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.service.MessageTimeline

internal fun MessagesUiState.withSentMessage(
    conversationId: ConversationId,
    message: Message
): Result<MessagesUiState> {
    val conversation = conversations.firstOrNull { it.id == conversationId }
        ?: return Result.Success(copy(isSending = false))
    return when (val updated = conversation.recordMessage(message)) {
        is Result.Error -> updated
        is Result.Success -> Result.Success(
            copy(
                conversations = conversations.map { existing ->
                    if (existing.id == conversationId) updated.data else existing
                }.sortedByDescending { it.updatedAtMillis },
                currentMessages = if (selectedConversationId == conversationId) {
                    MessageTimeline.merge(currentMessages, listOf(message))
                } else {
                    currentMessages
                },
                messageInput = if (selectedConversationId == conversationId) "" else messageInput,
                isSending = false
            )
        )
    }
}
