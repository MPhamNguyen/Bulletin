package com.jdrms.bulletin.domain.messages.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.GetConversationMessages
import com.jdrms.bulletin.domain.messages.application.GetConversations
import com.jdrms.bulletin.domain.messages.application.MessageSenderLookupException
import com.jdrms.bulletin.domain.messages.application.MessagingAuthenticationRequiredException
import com.jdrms.bulletin.domain.messages.application.ReportMessage
import com.jdrms.bulletin.domain.messages.application.SendMessage
import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MessagesViewModel(
    private val getConversations: GetConversations,
    private val getConversationMessages: GetConversationMessages,
    private val sendMessage: SendMessage,
    private val reportMessage: ReportMessage
) : ViewModel() {
    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null

    init {
        loadConversations()
    }

    fun loadConversations() = refreshConversations()

    private fun refreshConversations(statusMessage: String? = null) {
        loadJob?.cancel()
        val previousSelection = _uiState.value.selectedConversationId
        _uiState.update { MessagesUiState(isLoading = true, statusMessage = statusMessage) }
        loadJob = viewModelScope.launch {
            when (val result = getConversations()) {
                is Result.Error -> showFailure(result)
                is Result.Success -> {
                    val selected = result.data.firstOrNull { it.id == previousSelection } ?: result.data.firstOrNull()
                    _uiState.update {
                        it.copy(conversations = result.data, selectedConversationId = selected?.id, isLoading = false)
                    }
                    selected?.let { loadMessages(it.id) }
                }
            }
        }
    }

    fun selectConversation(conversationId: ConversationId) {
        if (_uiState.value.conversations.none { it.id == conversationId }) return
        loadJob?.cancel()
        _uiState.update {
            it.copy(
                selectedConversationId = conversationId,
                currentMessages = emptyList(),
                isLoadingMessages = true,
                errorMessage = null,
                statusMessage = null
            )
        }
        loadJob = viewModelScope.launch { loadMessages(conversationId) }
    }

    private suspend fun loadMessages(conversationId: ConversationId) {
        _uiState.update { it.copy(isLoadingMessages = true) }
        when (val result = getConversationMessages(conversationId)) {
            is Result.Success -> _uiState.update { it.copy(currentMessages = result.data, isLoadingMessages = false) }
            is Result.Error -> showFailure(result)
        }
    }

    fun onMessageInputChanged(input: String) {
        _uiState.update { it.copy(messageInput = input, errorMessage = null, statusMessage = null) }
    }

    fun sendCurrentMessage() {
        val state = _uiState.value
        val activeConvId = state.selectedConversationId ?: return
        if (state.isSending || state.isLoading || state.messageInput.isBlank()) return
        val text = state.messageInput
        _uiState.update { it.copy(isSending = true, errorMessage = null, statusMessage = null) }
        viewModelScope.launch {
            when (val result = sendMessage(activeConvId, text)) {
                is Result.Success -> refreshConversations("Message sent.")
                is Result.Error -> showFailure(result)
            }
        }
    }

    fun report(messageId: MessageId, reason: String = "Inappropriate content") {
        val state = _uiState.value
        val activeConvId = state.selectedConversationId ?: return
        if (state.isReporting || state.isLoading) return
        _uiState.update { it.copy(isReporting = true, errorMessage = null, statusMessage = null) }
        viewModelScope.launch {
            when (val result = reportMessage(activeConvId, messageId, reason)) {
                is Result.Success -> {
                    if (_uiState.value.selectedConversationId == activeConvId) {
                        selectConversation(activeConvId)
                        _uiState.update { it.copy(statusMessage = "Message reported.") }
                    }
                    _uiState.update { it.copy(isReporting = false) }
                }
                is Result.Error -> showFailure(result)
            }
        }
    }

    private fun showFailure(error: Result.Error) {
        val identityUnavailable = error.exception is MessagingAuthenticationRequiredException ||
            error.exception is MessageSenderLookupException || error.exception is ConversationAccessException
        if (identityUnavailable) loadJob?.cancel()
        val message = if (error.exception is MessagingAuthenticationRequiredException) {
            "Sign in to access your messages."
        } else {
            "Unable to complete the messaging request. Please try again."
        }
        _uiState.update {
            if (identityUnavailable) {
                MessagesUiState(errorMessage = message)
            } else {
                it.copy(
                    isLoading = false,
                    isLoadingMessages = false,
                    isSending = false,
                    isReporting = false,
                    errorMessage = message,
                    statusMessage = null
                )
            }
        }
    }
}
