package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.MessageCursor

/** Captures the history operation and its original paging boundary for an explicit retry. */
sealed interface MessageHistoryRequest {
    val conversationId: ConversationId

    data class Thread(
        override val conversationId: ConversationId,
        val through: MessageCursor?
    ) : MessageHistoryRequest

    data class Older(
        override val conversationId: ConversationId,
        val before: MessageCursor
    ) : MessageHistoryRequest
}
