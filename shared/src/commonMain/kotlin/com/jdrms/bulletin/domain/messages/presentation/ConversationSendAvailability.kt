package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.ConversationLockReason

internal fun MessagesUiState.canSendToCounterpart(): Boolean {
    val conversation = conversations.firstOrNull { it.id == selectedConversationId } ?: return false
    if (isSelectedConversationReadOnly || conversation.authorizeSending(viewerId) is Result.Error) return false
    val participant = conversation.counterpart(viewerId) ?: return false
    return !block.isBlocked(participant.id) &&
        (!block.statusRequired || participant.id in block.checkedUserIds) &&
        block.flow !is ConversationBlockFlow.Submitting
}

internal val MessagesUiState.isSelectedConversationReadOnly: Boolean
    get() = selectedConversationLockReason != null

internal val MessagesUiState.selectedConversationLockReason: ConversationLockReason?
    get() = if (currentMessages.any { it.conversationId == selectedConversationId && it.senderId == null }) {
        ConversationLockReason.ACCOUNT_DELETED
    } else {
        conversations.firstOrNull { it.id == selectedConversationId }?.lockReason
    }
