package com.jdrms.bulletin.domain.messages.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.GetConversationMessages
import com.jdrms.bulletin.domain.messages.application.GetConversations
import com.jdrms.bulletin.domain.messages.application.ObserveConversationMessages
import com.jdrms.bulletin.domain.messages.application.ReportMessage
import com.jdrms.bulletin.domain.messages.application.SendMessage
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.service.MessageTimeline
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MessagesViewModel(
    private val getConversations: GetConversations,
    private val getConversationMessages: GetConversationMessages,
    private val observeConversationMessages: ObserveConversationMessages,
    private val sendMessage: SendMessage,
    private val reportMessage: ReportMessage
) : ViewModel() {
    private val _uiState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = _uiState.asStateFlow()
    private var loadJob: Job? = null
    private var requestedConversationId: ConversationId? = null
    private var olderJob: Job? = null
    private var observeJob: Job? = null
    private var destinationActive = false
    private var initialPageLoaded = false
    private val streamCoordinator = MessageStreamCoordinator(observeConversationMessages)
    private val streamCallbacks = MessagesStreamCallbacksFactory(
        _uiState,
        ::reloadSelectedConversation,
        ::showFailure,
        { !initialPageLoaded }
    )
    override fun onCleared() {
        viewModelScope.cancel()
        super.onCleared()
    }

    init { loadConversations() }
    fun loadConversations() = refreshConversations()

    fun onDestinationActiveChanged(isActive: Boolean) {
        if (destinationActive == isActive) return
        destinationActive = isActive
        if (isActive) {
            refreshConversations()
        } else {
            observeJob?.cancel()
            observeJob = null
        }
    }

    private fun refreshConversations(statusMessage: String? = null) {
        loadJob?.cancel()
        olderJob?.cancel()
        observeJob?.cancel()
        initialPageLoaded = false
        val previousSelection = _uiState.value.selectedConversationId
        _uiState.update { MessagesUiState(isLoading = true, statusMessage = statusMessage) }
        loadJob = viewModelScope.launch {
            when (val result = getConversations()) {
                is Result.Error -> showFailure(result)
                is Result.Success -> {
                    val conversations = result.data.conversations
                    val requested = requestedConversationId
                    val selected = conversations.firstOrNull { it.id == requested }
                        ?: conversations.firstOrNull { it.id == previousSelection }
                        ?: conversations.firstOrNull()
                    if (selected?.id == requested) requestedConversationId = null
                    _uiState.update {
                        it.copy(
                            viewerId = result.data.viewerId,
                            conversations = conversations,
                            selectedConversationId = selected?.id,
                            isLoading = false
                        )
                    }
                    if (destinationActive) selected?.let { observeConversation(it.id) }
                }
            }
        }
    }

    fun selectConversation(conversationId: ConversationId) {
        if (_uiState.value.conversations.none { it.id == conversationId }) return
        loadJob?.cancel()
        olderJob?.cancel()
        observeJob?.cancel()
        initialPageLoaded = false
        _uiState.update {
            it.copy(
                selectedConversationId = conversationId,
                currentMessages = emptyList(),
                revealedReportedMessageIds = emptySet(),
                olderCursor = null,
                isLoadingMessages = true,
                isLoadingOlder = false,
                errorMessage = null,
                statusMessage = null
            )
        }
        observeConversation(conversationId)
    }

    private fun observeConversation(conversationId: ConversationId) {
        _uiState.update { it.copy(connectionState = MessageConnectionState.CONNECTING) }
        observeJob = streamCoordinator.observe(
            viewModelScope,
            conversationId,
            streamCallbacks.forConversation(conversationId)
        )
    }

    private fun reloadSelectedConversation(conversationId: ConversationId) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch { loadMessages(conversationId) }
    }

    fun openConversation(conversationId: ConversationId) = run {
        requestedConversationId = conversationId
        loadConversations()
    }

    private suspend fun loadMessages(conversationId: ConversationId) {
        _uiState.update { it.copy(isLoadingMessages = true) }
        val result = getConversationMessages(conversationId)
        if (_uiState.value.selectedConversationId != conversationId) return
        when (result) {
            is Result.Success -> {
                val conversation = _uiState.value.conversations.firstOrNull { it.id == conversationId }
                if (conversation == null) {
                    _uiState.update { it.clearMessageLoadingForMissingConversation(conversationId) }
                    return
                }
                when (val reconciled = conversation.reconcileMessages(result.data.messages)) {
                    is Result.Error -> showFailure(reconciled)
                    is Result.Success -> _uiState.update { state ->
                        state.copy(
                            conversations = state.conversations.map { existing ->
                                if (existing.id == conversationId) reconciled.data else existing
                            }.sortedByDescending { it.updatedAtMillis },
                            currentMessages = MessageTimeline.merge(state.currentMessages, result.data.messages),
                            olderCursor = if (initialPageLoaded) state.olderCursor else result.data.olderCursor,
                            isLoadingMessages = false,
                            isLoadingOlder = false
                        )
                    }
                }
                if (_uiState.value.selectedConversationId == conversationId) initialPageLoaded = true
            }
            is Result.Error -> showFailure(result)
        }
    }

    fun loadOlderMessages() {
        val selected = _uiState.value.selectedConversationId ?: return
        val cursor = _uiState.value.olderCursor ?: return
        if (olderJob?.isActive == true) return
        _uiState.update { it.copy(isLoadingOlder = true) }
        olderJob = viewModelScope.launch {
            when (val result = getConversationMessages(selected, cursor)) {
                is Result.Success -> _uiState.update {
                    if (it.selectedConversationId == selected && it.olderCursor == cursor) {
                        it.copy(
                            currentMessages = MessageTimeline.merge(it.currentMessages, result.data.messages),
                            olderCursor = result.data.olderCursor,
                            isLoadingOlder = false
                        )
                    } else {
                        it
                    }
                }
                is Result.Error -> showFailure(result)
            }
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
                is Result.Success -> when (val updated = _uiState.value.withSentMessage(activeConvId, result.data)) {
                    is Result.Error -> showFailure(updated)
                    is Result.Success -> _uiState.update { updated.data }
                }
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
        val presented = _uiState.value.withFailure(error)
        if (presented.identityUnavailable) {
            loadJob?.cancel()
            olderJob?.cancel()
            observeJob?.cancel()
        }
        _uiState.update { presented.state }
    }
}
