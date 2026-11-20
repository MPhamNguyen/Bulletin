package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.application.ConversationListingSales
import com.jdrms.bulletin.domain.messages.application.ConversationUserBlocker
import com.jdrms.bulletin.domain.messages.application.EditMessage
import com.jdrms.bulletin.domain.messages.application.MarkConversationRead
import com.jdrms.bulletin.domain.messages.application.SubmitConversationReview

/** Optional thread interactions for callers that only need to display or send messages. */
data class MessageThreadOperations(
    val markRead: MarkConversationRead? = null,
    val edit: EditMessage? = null,
    val userBlocker: ConversationUserBlocker? = null,
    val listingSales: ConversationListingSales? = null,
    val submitReview: SubmitConversationReview? = null
)
