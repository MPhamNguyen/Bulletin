package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result

internal fun MessagesUiState.canSendToCounterpart(): Boolean {
    val conversation = conversations.firstOrNull { it.id == selectedConversationId } ?: return false
    if (isSelectedConversationReadOnly || conversation.authorizeSending(viewerId) is Result.Error) return false
    val participant = conversation.counterpart(viewerId) ?: return false
    return !block.isBlocked(participant.id) &&
        (!block.statusRequired || participant.id in block.checkedUserIds) &&
        block.flow !is ConversationBlockFlow.Submitting
}

internal val MessagesUiState.isSelectedConversationReadOnly: Boolean
    get() = conversations.firstOrNull { it.id == selectedConversationId }?.isReadOnly == true ||
        currentMessages.any { it.conversationId == selectedConversationId && it.senderId == null }
