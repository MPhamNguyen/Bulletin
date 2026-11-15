package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.HideConversation
import com.jdrms.bulletin.domain.messages.application.ReportMessage
import com.jdrms.bulletin.domain.messages.application.SendMessage
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.reporting.domain.model.ReportReason
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class MessagesActions(
    private val inbox: InboxStateOwner,
    private val state: MutableStateFlow<MessagesUiState>,
    sendMessage: SendMessage,
    private val reportMessage: ReportMessage,
    private val hideConversation: HideConversation,
    private val scope: CoroutineScope,
    drafts: ConversationMessageDrafts
) {
    private var hideJob: kotlinx.coroutines.Job? = null
    private val sending = MessageSendingActions(inbox, state, sendMessage, drafts, scope)

    fun send(showFailure: (Result.Error) -> Unit) = sending.send(showFailure)

    fun report(
        messageId: MessageId,
        reason: ReportReason,
        description: String,
        onReported: (ConversationId) -> Unit,
        showFailure: (Result.Error) -> Unit
    ) {
        val snapshot = state.value
        val conversationId = snapshot.selectedConversationId ?: return
        val viewerId = inbox.state.value.viewerId ?: return
        val targetMessage = snapshot.currentMessages.firstOrNull { it.id == messageId } ?: return
        val isReportBlocked = !targetMessage.canBeReportedBy(viewerId, conversationId) ||
            snapshot.isReporting || inbox.state.value.isLoading
        if (isReportBlocked) return
        state.update { it.copy(isReporting = true, errorMessage = null, statusMessage = null) }
        scope.launch {
            when (val result = reportMessage(conversationId, messageId, reason, description)) {
                is Result.Error -> showFailure(result)
                is Result.Success -> {
                    if (state.value.selectedConversationId == conversationId) {
                        onReported(conversationId)
                    }
                    state.update { it.copy(isReporting = false) }
                }
            }
        }
    }

    private fun Message.canBeReportedBy(viewerId: SenderId, conversationId: ConversationId): Boolean =
        senderId != viewerId && this.conversationId == conversationId && !isReported

    fun toggleVisibility(messageId: MessageId) {
        state.update { current ->
            if (current.currentMessages.none { it.id == messageId && it.isReported }) {
                current
            } else {
                current.copy(
                    revealedReportedMessageIds = current.revealedReportedMessageIds.let { ids ->
                        if (messageId in ids) ids - messageId else ids + messageId
                    }
                )
            }
        }
    }

    fun hideConversation(
        conversationId: ConversationId,
        showFailure: (Result.Error) -> Unit
    ) {
        if (inbox.state.value.conversations.none { it.id == conversationId } || hideJob?.isActive == true) return
        hideJob = scope.launch {
            when (val result = hideConversation(conversationId)) {
                is Result.Error -> showFailure(result)
                is Result.Success -> {
                    state.update {
                        it.copy(
                            errorMessage = null,
                            statusMessage = "Conversation removed from your inbox."
                        )
                    }
                    inbox.refresh()
                }
            }
        }
    }
}
