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
import com.jdrms.bulletin.domain.messages.domain.model.Message
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
                    val conversations = result.data.conversations
                    val selected = conversations.firstOrNull { it.id == previousSelection }
                        ?: conversations.firstOrNull()
                    _uiState.update {
                        it.copy(
                            viewerId = result.data.viewerId,
                            conversations = conversations,
                            selectedConversationId = selected?.id,
                            isLoading = false
                        )
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
                revealedReportedMessageIds = emptySet(),
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
            is Result.Success -> {
                val conversation = _uiState.value.conversations.firstOrNull { it.id == conversationId } ?: return
                when (val reconciled = conversation.reconcileMessages(result.data)) {
                    is Result.Error -> showFailure(reconciled)
                    is Result.Success -> _uiState.update { state ->
                        state.copy(
                            conversations = state.conversations.map { existing ->
                                if (existing.id == conversationId) reconciled.data else existing
                            }.sortedByDescending { it.updatedAtMillis },
                            currentMessages = result.data,
                            isLoadingMessages = false
                        )
                    }
                }
            }
            is Result.Error -> showFailure(result)
        }
    }

    fun onMessageInputChanged(input: String) {
        _uiState.update { it.copy(messageInput = input, errorMessage = null, statusMessage = null) }
    }

    fun toggleReportedMessageVisibility(messageId: MessageId) {
        _uiState.update { state ->
            if (state.currentMessages.none { it.id == messageId && it.isReported }) {
                state
            } else {
                val revealed = state.revealedReportedMessageIds
                val nextRevealed = if (messageId in revealed) revealed - messageId else revealed + messageId
                state.copy(revealedReportedMessageIds = nextRevealed)
            }
        }
    }

    fun sendCurrentMessage() {
        val state = _uiState.value
        val activeConvId = state.selectedConversationId ?: return
        if (state.isSending || state.isLoading) return
        if (state.isLoadingMessages || state.messageInput.isBlank()) return
        val text = state.messageInput
        _uiState.update { it.copy(isSending = true, errorMessage = null, statusMessage = null) }
        viewModelScope.launch {
            when (val result = sendMessage(activeConvId, text)) {
                is Result.Success -> applySentMessage(activeConvId, result.data)
                is Result.Error -> showFailure(result)
            }
        }
    }

    private fun applySentMessage(conversationId: ConversationId, message: Message) {
        val conversation = _uiState.value.conversations.firstOrNull { it.id == conversationId }
        when (val updated = conversation?.recordMessage(message)) {
            is Result.Error -> showFailure(updated)
            is Result.Success -> _uiState.update { current ->
                val visibleMessages = if (current.selectedConversationId == conversationId &&
                    current.currentMessages.none { it.id == message.id }
                ) {
                    current.currentMessages + message
                } else {
                    current.currentMessages
                }
                current.copy(
                    conversations = current.conversations.map { existing ->
                        if (existing.id == conversationId) updated.data else existing
                    }.sortedByDescending { it.updatedAtMillis },
                    currentMessages = visibleMessages,
                    messageInput = if (current.selectedConversationId == conversationId) "" else current.messageInput,
                    isSending = false,
                    statusMessage = "Message sent."
                )
            }
            null -> _uiState.update { it.copy(isSending = false) }
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
