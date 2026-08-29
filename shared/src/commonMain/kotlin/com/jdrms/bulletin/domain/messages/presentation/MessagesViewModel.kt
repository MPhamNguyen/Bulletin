package com.jdrms.bulletin.domain.messages.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.generateUuid
import com.jdrms.bulletin.domain.messages.application.GetConversationMessages
import com.jdrms.bulletin.domain.messages.application.GetConversations
import com.jdrms.bulletin.domain.messages.application.ReportMessage
import com.jdrms.bulletin.domain.messages.application.SendMessage
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MessagesViewModel(
    private val getConversations: GetConversations,
    private val getConversationMessages: GetConversationMessages,
    private val sendMessage: SendMessage,
    private val reportMessage: ReportMessage,
    private val currentUserId: String = "current_student",
    private val currentUserName: String = "Dominic Alfonso"
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
    }

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val convs = getConversations(currentUserId)
            val selectedId = _uiState.value.selectedConversationId ?: convs.firstOrNull()?.id
            _uiState.update { it.copy(conversations = convs, selectedConversationId = selectedId, isLoading = false) }
            selectedId?.let { loadMessages(it) }
        }
    }

    fun selectConversation(conversationId: ConversationId) {
        _uiState.update { it.copy(selectedConversationId = conversationId) }
        loadMessages(conversationId)
    }

    private fun loadMessages(conversationId: ConversationId) {
        viewModelScope.launch {
            val messages = getConversationMessages(conversationId)
            _uiState.update { it.copy(currentMessages = messages) }
        }
    }

    fun onMessageInputChanged(input: String) {
        _uiState.update { it.copy(messageInput = input) }
    }

    fun sendCurrentMessage() {
        val activeConvId = _uiState.value.selectedConversationId ?: return
        val text = _uiState.value.messageInput.trim()
        if (text.isBlank()) return

        val newMessage = Message(
            id = MessageId("msg_${generateUuid().take(8)}"),
            conversationId = activeConvId,
            senderId = SenderId(currentUserId),
            senderName = currentUserName,
            content = text,
            timestampMillis = currentTimeMillis()
        )

        viewModelScope.launch {
            val result = sendMessage(activeConvId, newMessage)
            if (result.isSuccess()) {
                _uiState.update { it.copy(messageInput = "") }
                loadMessages(activeConvId)
                val convs = getConversations(currentUserId)
                _uiState.update { it.copy(conversations = convs) }
            }
        }
    }

    fun report(messageId: MessageId, reason: String = "Inappropriate content") {
        viewModelScope.launch {
            val result = reportMessage(messageId, reason)
            if (result.isSuccess()) {
                val activeConvId = _uiState.value.selectedConversationId
                activeConvId?.let { loadMessages(it) }
            }
        }
    }
}
