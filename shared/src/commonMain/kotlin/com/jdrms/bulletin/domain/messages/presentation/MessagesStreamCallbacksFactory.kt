package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.service.MessageTimeline
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class MessagesStreamCallbacksFactory(
    private val uiState: MutableStateFlow<MessagesUiState>,
    private val inbox: InboxStateOwner,
    private val reloadSelectedConversation: (ConversationId) -> Unit,
    private val showFailure: (Result.Error) -> Unit,
    private val shouldReloadAfterFailure: () -> Boolean,
    private val onConversationRead: (ConversationId) -> Unit = {}
) {
    private var pendingConversationId: ConversationId? = null
    private var pendingViewerId: SenderId? = null
    private val pendingMessages = linkedMapOf<MessageId, Message>()

    fun hasPendingFor(conversationId: ConversationId): Boolean =
        pendingConversationId == conversationId && pendingMessages.isNotEmpty()

    fun isWaitingForInbox(
        snapshot: InboxState,
        current: ConversationId?,
        requested: ConversationId?
    ): Boolean {
        val pending = pendingConversationId ?: return false
        return pendingMessages.isNotEmpty() && (pending == current || pending == requested) &&
            snapshot.conversations.none { it.id == pending }
    }

    fun clearPending() {
        pendingConversationId = null
        pendingViewerId = null
        pendingMessages.clear()
    }

    fun onInboxSnapshot(snapshot: InboxState) {
        val conversationId = pendingConversationId ?: return
        if (snapshot.viewerId != pendingViewerId) {
            clearPending()
            return
        }
        if (uiState.value.selectedConversationId != conversationId) return
        val conversation = snapshot.conversations.firstOrNull { it.id == conversationId } ?: return
        val messages = pendingMessages.values.toList()
        clearPending()
        messages.forEach { deliver(it, conversation) }
        reloadSelectedConversation(conversationId)
    }

    fun forConversation(conversationId: ConversationId) = MessageStreamCallbacks(
        isCurrent = { uiState.value.selectedConversationId == conversationId },
        onConnected = {
            uiState.update { it.copy(connectionState = MessageConnectionState.CONNECTED) }
            reloadSelectedConversation(conversationId)
        },
        onConnectionState = { connectionState ->
            uiState.update { it.copy(connectionState = connectionState) }
        },
        onReceived = onReceived@{ message ->
            val conversation = inbox.state.value.conversations.firstOrNull { it.id == conversationId }
            if (conversation == null) {
                val viewerId = inbox.state.value.viewerId ?: return@onReceived
                if (pendingConversationId != conversationId || pendingViewerId != viewerId) clearPending()
                pendingConversationId = conversationId
                pendingViewerId = viewerId
                pendingMessages[message.id] = message
                if (pendingMessages.size > MAX_PENDING_MESSAGES) pendingMessages.remove(pendingMessages.keys.first())
                inbox.refresh()
                return@onReceived
            }
            deliver(message, conversation)
        },
        onFailed = { cause, terminal ->
            if (terminal) {
                clearPending()
                showFailure(Result.Error(cause))
            } else {
                uiState.update { it.copy(connectionState = MessageConnectionState.DISCONNECTED) }
                if (shouldReloadAfterFailure()) reloadSelectedConversation(conversationId)
            }
        }
    )

    private fun deliver(message: Message, conversation: Conversation) {
        val viewerId = inbox.state.value.viewerId ?: return
        if (!conversation.includes(viewerId) || !conversation.canSee(message)) return
        val senderName = conversation.participants.firstOrNull { it.id == message.senderId }?.displayName
        val resolvedMessage = senderName?.let { message.copy(senderName = it) } ?: message
        when (val recorded = inbox.recordMessage(resolvedMessage)) {
            is Result.Error -> showFailure(recorded)
            is Result.Success -> {
                uiState.update {
                    it.copy(currentMessages = MessageTimeline.merge(it.currentMessages, listOf(resolvedMessage)))
                }
                if (message.senderId != viewerId && uiState.value.isConversationOpen) {
                    onConversationRead(conversation.id)
                }
            }
        }
    }
}

private const val MAX_PENDING_MESSAGES = 100
