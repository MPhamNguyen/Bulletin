package com.jdrms.bulletin.domain.messaging.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messaging.application.GetConversations
import com.jdrms.bulletin.domain.messaging.application.ReportMessage
import com.jdrms.bulletin.domain.messaging.application.SendMessage
import com.jdrms.bulletin.domain.messaging.domain.model.ConversationId
import com.jdrms.bulletin.domain.messaging.domain.model.MessageId
import com.jdrms.bulletin.domain.messaging.domain.model.ParticipantId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MessagingViewModel(
    private val getConversations: GetConversations,
    private val sendMessage: SendMessage,
    private val reportMessage: ReportMessage,
    private val currentUserId: ParticipantId = ParticipantId("user_101"),
    private val currentUserName: String = "Dominic Alfonso"
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessagingUiState())
    val uiState: StateFlow<MessagingUiState> = _uiState.asStateFlow()

    init {
        loadConversations()
    }

    fun loadConversations() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val convs = getConversations.getConversations(currentUserId)
            val selected = _uiState.value.selectedConversationId ?: convs.firstOrNull()?.id
            _uiState.update { it.copy(conversations = convs, selectedConversationId = selected, isLoading = false) }
            selected?.let { loadMessages(it) }
        }
    }

    fun selectConversation(id: ConversationId) {
        _uiState.update { it.copy(selectedConversationId = id) }
        loadMessages(id)
    }

    fun loadMessages(id: ConversationId) {
        viewModelScope.launch {
            val msgs = getConversations.getMessages(id)
            _uiState.update { it.copy(currentMessages = msgs) }
        }
    }

    fun onMessageInputChanged(text: String) {
        _uiState.update { it.copy(messageInput = text) }
    }

    fun sendCurrentMessage() {
        val convId = _uiState.value.selectedConversationId ?: return
        val text = _uiState.value.messageInput
        if (text.isBlank()) return

        viewModelScope.launch {
            when (val res = sendMessage(convId, currentUserId, currentUserName, text)) {
                is Result.Success -> {
                    _uiState.update { it.copy(messageInput = "") }
                    loadMessages(convId)
                    loadConversations()
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = res.message) }
                }
            }
        }
    }

    fun report(messageId: MessageId, reason: String) {
        viewModelScope.launch {
            when (val res = reportMessage(messageId, reason)) {
                is Result.Success -> {
                    _uiState.value.selectedConversationId?.let { loadMessages(it) }
                }
                is Result.Error -> {
                    _uiState.update { it.copy(errorMessage = res.message) }
                }
            }
        }
    }
}
