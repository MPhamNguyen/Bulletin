package com.jdrms.bulletin.domain.messages.presentation

internal sealed interface ConversationReviewState {
    data object Ready : ConversationReviewState
    data object Submitting : ConversationReviewState
    data object Submitted : ConversationReviewState
    data class Failed(val message: String) : ConversationReviewState
}
