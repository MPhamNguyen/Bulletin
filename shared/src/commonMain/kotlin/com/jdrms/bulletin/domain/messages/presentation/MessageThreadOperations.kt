package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.application.EditMessage
import com.jdrms.bulletin.domain.messages.application.MarkConversationRead

/** Optional thread interactions for callers that only need to display or send messages. */
data class MessageThreadOperations(
    val markRead: MarkConversationRead? = null,
    val edit: EditMessage? = null
)
