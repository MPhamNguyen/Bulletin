package com.jdrms.bulletin.domain.messages.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.GetConversationMessages
import com.jdrms.bulletin.domain.messages.application.HideConversation
import com.jdrms.bulletin.domain.messages.application.ObserveConversationMessages
import com.jdrms.bulletin.domain.messages.application.ReportMessage
import com.jdrms.bulletin.domain.messages.application.SendMessage
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.service.MessageTimeline
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MessagesViewModel(
    private val inbox: InboxStateOwner,
    private val getConversationMessages: GetConversationMessages,
    private val hideConversation: HideConversation,
    observeConversationMessages: ObserveConversationMessages,
    sendMessage: SendMessage,
    reportMessage: ReportMessage
) : ViewModel() {
    private val threadState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = MessagesUiStateProjection(threadState, inbox.state, viewModelScope)
    val inboxState: StateFlow<InboxState> = inbox.state
    private var loadJob: Job? = null
    private var requestedConversationId: ConversationId? = null
    private var hideJob: Job? = null
    private var observeJob: Job? = null
    private var destinationActive = false
    private var visibleViewerId: SenderId? = null
    private var initialPageLoaded = false
    private val showFailure: (Result.Error) -> Unit = { error ->
        val presented = threadState.value.withFailure(error)
        if (presented.identityUnavailable) {
            loadJob?.cancel()
            olderJob?.cancel()
            observeJob?.cancel()
        }
        threadState.value = presented.state
    }
    private val actions = MessagesActions(inbox, threadState, sendMessage, reportMessage, viewModelScope)
    private val streamCoordinator = MessageStreamCoordinator(observeConversationMessages)
    private val olderHistory = MessagesOlderHistoryLoader(
        _uiState,
        getConversationMessages,
        viewModelScope,
        ::showFailure
    )
    private val streamCallbacks = MessagesStreamCallbacksFactory(
        threadState,
        inbox,
        ::reloadSelectedConversation,
        showFailure,
        { !initialPageLoaded }
    )

    init { viewModelScope.launch { inbox.state.collect(::applyInboxSelection) } }

    fun loadConversations() {
        inbox.refresh()
        applyInboxSelection(inbox.state.value)
    }

    fun onDestinationActiveChanged(isActive: Boolean) {
        if (destinationActive == isActive) return
        destinationActive = isActive
        if (isActive) {
            inbox.refresh()
            threadState.value.selectedConversationId?.let { conversationId ->
                reloadSelectedConversation(conversationId)
                observeConversation(conversationId)
            }
        } else {
            observeJob?.cancel()
            observeJob = null
        }
    }

    private fun applyInboxSelection(snapshot: InboxState) {
        if (snapshot.viewerId != visibleViewerId) {
            val previousViewerId = visibleViewerId
            visibleViewerId = snapshot.viewerId
            loadJob?.cancel()
            olderJob?.cancel()
            observeJob?.cancel()
            observeJob = null
            requestedConversationId = null
            initialPageLoaded = false
            threadState.value = MessagesUiState()
            if (previousViewerId != null && snapshot.viewerId != null) {
                threadState.update { it.copy(errorMessage = "Conversation is unavailable for this user.") }
            }
        }
        if (snapshot.viewerId == null) return
        if (snapshot.isLoading) return
        val current = threadState.value.selectedConversationId
        val selected = snapshot.conversations.firstOrNull { it.id == requestedConversationId }
            ?: snapshot.conversations.firstOrNull { it.id == current }
            ?: snapshot.conversations.firstOrNull()
        if (selected?.id == requestedConversationId) requestedConversationId = null
        if (selected?.id != current) selectLoadedConversation(selected?.id)
    }

    private fun selectLoadedConversation(conversationId: ConversationId?) {
        loadJob?.cancel()
        olderHistory.cancel()
        observeJob?.cancel()
        observeJob = null
        initialPageLoaded = false
        threadState.update {
            it.copy(
                selectedConversationId = conversationId,
                currentMessages = emptyList(),
                revealedReportedMessageIds = emptySet(),
                olderCursor = null,
                isLoadingMessages = conversationId != null,
                isLoadingOlder = false,
                errorMessage = null,
                statusMessage = null
            )
        }
        if (destinationActive && conversationId != null) {
            reloadSelectedConversation(conversationId)
            observeConversation(conversationId)
        }
    }

    fun selectConversation(conversationId: ConversationId) {
        if (inbox.state.value.conversations.none { it.id == conversationId }) return
        selectLoadedConversation(conversationId)
    }

    fun openConversation(conversationId: ConversationId) {
        requestedConversationId = conversationId
        applyInboxSelection(inbox.state.value)
        inbox.refresh()
    }

    private fun observeConversation(conversationId: ConversationId) {
        observeJob?.cancel()
        threadState.update { it.copy(connectionState = MessageConnectionState.CONNECTING) }
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

    private suspend fun loadMessages(conversationId: ConversationId) {
        threadState.update { it.copy(isLoadingMessages = true) }
        val result = getConversationMessages(conversationId)
        if (threadState.value.selectedConversationId != conversationId) return
        when (result) {
            is Result.Error -> showFailure(result)
            is Result.Success -> {
                if (inbox.state.value.conversations.none { it.id == conversationId }) {
                    threadState.update { it.clearMessageLoadingForMissingConversation(conversationId) }
                    return
                }
                when (val reconciled = inbox.reconcileMessages(conversationId, result.data.messages)) {
                    is Result.Error -> showFailure(reconciled)
                    is Result.Success -> threadState.update { state ->
                        state.copy(
                            currentMessages = MessageTimeline.merge(state.currentMessages, result.data.messages),
                            olderCursor = if (initialPageLoaded) state.olderCursor else result.data.olderCursor,
                            isLoadingMessages = false,
                            isLoadingOlder = false
                        )
                    }
                }
                initialPageLoaded = true
            }
        }
    }

    fun loadOlderMessages() {
        val selected = threadState.value.selectedConversationId ?: return
        val cursor = threadState.value.olderCursor ?: return
        if (olderJob?.isActive == true) return
        threadState.update { it.copy(isLoadingOlder = true) }
        olderJob = viewModelScope.launch {
            when (val result = getConversationMessages(selected, cursor)) {
                is Result.Error -> showFailure(result)
                is Result.Success -> threadState.update {
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
            }
        }
    }
    
    fun hideConversationFromInbox(conversationId: ConversationId) {
    if (
        inbox.state.value.conversations.none { it.id == conversationId } ||
        hideJob?.isActive == true
    ) {
        return
    }

    hideJob = viewModelScope.launch {
        when (val result = hideConversation(conversationId)) {
            is Result.Error -> showFailure(result)
            is Result.Success -> {
                threadState.update {
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

    fun onMessageInputChanged(input: String) {
        threadState.update { it.copy(messageInput = input, errorMessage = null, statusMessage = null) }
    }

    fun toggleReportedMessageVisibility(messageId: MessageId) {
        threadState.update { state ->
            if (state.currentMessages.none { it.id == messageId && it.isReported }) {
                state
            } else {
                state.copy(
                    revealedReportedMessageIds = state.revealedReportedMessageIds.let { ids ->
                        if (messageId in ids) ids - messageId else ids + messageId
                    }
                )
            }
        }
    }

    fun sendCurrentMessage() {
        inbox.synchronizeIdentity()
        applyInboxSelection(inbox.state.value)
        actions.send(showFailure)
    }

    fun report(messageId: MessageId, reason: String = "Inappropriate content") {
        inbox.synchronizeIdentity()
        applyInboxSelection(inbox.state.value)
        actions.report(messageId, reason, ::selectConversation, showFailure)
    }
}
