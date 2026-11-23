package com.jdrms.bulletin.domain.messages.application

import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

data class MessageInbox(
    val viewerId: SenderId,
    val conversations: List<Conversation>,
    val pendingReviewConversationIds: Set<ConversationId>? = emptySet(),
    val unmatchedReviewRequests: List<PendingConversationReview>? = emptyList()
)
