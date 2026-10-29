package com.jdrms.bulletin.domain.messages.presentation

internal fun MessagesUiState.canSendToCounterpart(): Boolean {
    val participant = conversations.firstOrNull { it.id == selectedConversationId }?.counterpart(viewerId)
        ?: return false
    return !block.isBlocked(participant.id) &&
        (!block.statusRequired || participant.id in block.checkedUserIds) &&
        block.flow !is ConversationBlockFlow.Submitting
}
