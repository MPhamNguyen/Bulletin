package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.domain.model.Conversation

internal data class ConversationInboxAttention(
    val hasUnreadMessage: Boolean,
    val hasPendingReview: Boolean,
    val previewText: String
)

internal fun conversationInboxAttention(
    conversation: Conversation,
    state: MessagesUiState,
    reviews: Map<String, ConversationReviewState> = emptyMap()
): ConversationInboxAttention {
    val pendingReview = conversation.id in state.pendingReviewConversationIds &&
        reviews[conversation.listingId?.value] != ConversationReviewState.Submitted
    val viewer = state.viewerId
    val latest = conversation.lastMessage
    val readAt = conversation.participants.firstOrNull { it.id == viewer }?.lastReadAt
    val unreadMessage = latest != null && viewer != null && latest.senderId != viewer &&
        (readAt == null || latest.orderingTimestamp > readAt)
    return ConversationInboxAttention(
        hasUnreadMessage = pendingReview || unreadMessage,
        hasPendingReview = pendingReview,
        previewText = if (pendingReview) "Leave a review." else conversationPreviewText(conversation)
    )
}
