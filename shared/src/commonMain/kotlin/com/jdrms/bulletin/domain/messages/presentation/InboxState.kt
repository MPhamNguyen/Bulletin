package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.application.PendingConversationReview
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

data class InboxState(
    val viewerId: SenderId? = null,
    val conversations: List<Conversation> = emptyList(),
    val hasMore: Boolean = false,
    val isLoadingMore: Boolean = false,
    val isLoading: Boolean = true,
    val pendingReviewConversationIds: Set<ConversationId> = emptySet(),
    val unmatchedReviewRequests: List<PendingConversationReview> = emptyList(),
    val errorMessage: String? = null,
    val connectionState: InboxConnectionState = InboxConnectionState.CONNECTING
)

enum class InboxConnectionState { CONNECTING, CONNECTED, DISCONNECTED }
