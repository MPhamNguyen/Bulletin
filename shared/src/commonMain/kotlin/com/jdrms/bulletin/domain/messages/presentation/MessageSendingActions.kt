package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.SendMessage
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.service.MessageTimeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class MessageSendingActions(
    private val inbox: InboxStateOwner,
    private val state: MutableStateFlow<MessagesUiState>,
    private val sendMessage: SendMessage,
    private val drafts: ConversationMessageDrafts,
    private val scope: CoroutineScope
) {
    fun send(showFailure: (Result.Error) -> Unit) {
        val snapshot = state.value
        val conversationId = snapshot.selectedConversationId ?: return
        val viewerId = inbox.state.value.viewerId ?: return
        val session = drafts.session
        val busy = snapshot.isSending || inbox.state.value.isLoading || snapshot.isLoadingMessages
        if (busy || snapshot.editState != MessageEditState.Composing || snapshot.messageInput.isBlank()) return
        state.update {
            it.copy(
                sendStates = it.sendStates + (conversationId to MessageSendState.Sending),
                errorMessage = null,
                statusMessage = null
            )
        }
        scope.launch {
            // Identity may have changed before this coroutine started or while the send was pending.
            if (!synchronizeSession(viewerId, session)) return@launch
            val sent = sendMessage(conversationId, snapshot.messageInput)
            if (!synchronizeSession(viewerId, session)) return@launch
            when (sent) {
                is Result.Error -> if (sent.isMessagingIdentityUnavailable) {
                    showFailure(sent)
                } else {
                    finish(conversationId, "Unable to send message. Check your connection and try again.")
                }
                is Result.Success -> {
                    drafts.clearSent(conversationId, snapshot.messageInput)
                    complete(sent.data)
                }
            }
        }
    }

    private fun synchronizeSession(viewerId: SenderId, session: Any): Boolean {
        inbox.synchronizeIdentity()
        return drafts.session === session && inbox.state.value.viewerId == viewerId
    }

    private fun complete(message: Message) {
        inbox.recordMessage(message)
        state.update { current ->
            current.copy(
                currentMessages = if (current.selectedConversationId == message.conversationId) {
                    MessageTimeline.merge(current.currentMessages, listOf(message))
                } else {
                    current.currentMessages
                }
            )
        }
        finish(message.conversationId)
    }

    private fun finish(conversationId: ConversationId, errorMessage: String? = null) {
        state.update { current ->
            current.copy(
                sendStates = if (errorMessage == null) {
                    current.sendStates - conversationId
                } else {
                    current.sendStates + (conversationId to MessageSendState.Failed(errorMessage))
                }
            )
        }
    }
}
