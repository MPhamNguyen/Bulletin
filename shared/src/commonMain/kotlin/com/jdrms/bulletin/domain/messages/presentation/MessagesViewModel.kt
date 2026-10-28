package com.jdrms.bulletin.domain.messages.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.ConversationListingPreviewProvider
import com.jdrms.bulletin.domain.messages.application.EmptyConversationListingPreviewProvider
import com.jdrms.bulletin.domain.messages.application.GetConversationMessages
import com.jdrms.bulletin.domain.messages.application.HideConversation
import com.jdrms.bulletin.domain.messages.application.ObserveConversationMessages
import com.jdrms.bulletin.domain.messages.application.ReportMessage
import com.jdrms.bulletin.domain.messages.application.SendMessage
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.reporting.domain.model.ReportReason
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MessagesViewModel(
    private val inbox: InboxStateOwner,
    getConversationMessages: GetConversationMessages,
    observeConversationMessages: ObserveConversationMessages,
    sendMessage: SendMessage,
    reportMessage: ReportMessage,
    private val hideConversation: HideConversation,
    private val listingPreviewProvider: ConversationListingPreviewProvider = EmptyConversationListingPreviewProvider,
    threadOperations: MessageThreadOperations = MessageThreadOperations()
) : ViewModel() {
    private val threadState = MutableStateFlow(MessagesUiState())
    val uiState: StateFlow<MessagesUiState> = MessagesUiStateProjection(threadState, inbox.state, viewModelScope)
    val inboxState: StateFlow<InboxState> = inbox.state
    private var loadJob: Job? = null
    private var requestedConversationId: ConversationId? = null
    private val listingPreviewLoader =
        ConversationListingPreviewLoader(threadState, listingPreviewProvider, viewModelScope)
    private var observeJob: Job? = null
    private val destinationLifecycle = MessagesDestinationLifecycle(
        inbox,
        threadState,
        listingPreviewLoader,
        ::reloadSelectedConversation,
        ::observeConversation
    ) {
        observeJob?.cancel()
        observeJob = null
    }
    private var visibleViewerId: SenderId? = null
    private val showFailure: (Result.Error) -> Unit = { error ->
        val presented = threadState.value.withFailure(error)
        if (presented.identityUnavailable) {
            loadJob?.cancel()
            olderHistory.cancel()
            observeJob?.cancel()
        }
        threadState.value = presented.state
    }
    private val actions = MessagesActions(
        inbox,
        threadState,
        sendMessage,
        reportMessage,
        hideConversation,
        viewModelScope
    )
    private val messageReportActions = MessageReportActions(
        threadState,
        inbox,
        actions,
        ::selectConversation,
        showFailure
    )
    private val editing = MessageEditingActions(inbox, threadState, threadOperations.edit, viewModelScope)
    private val streamCoordinator = MessageStreamCoordinator(observeConversationMessages)
    private val olderHistory =
        MessagesOlderHistoryLoader(threadState, getConversationMessages, viewModelScope, showFailure)
    private val readReceiptHandler = ConversationReadReceiptHandler(
        inbox,
        threadState,
        { visibleViewerId },
        threadOperations.markRead,
        viewModelScope,
        showFailure
    )
    private val messagePageLoader = ConversationMessagePageLoader(
        threadState,
        inbox,
        getConversationMessages,
        readReceiptHandler,
        showFailure
    )
    private val conversationSelection = MessagesConversationSelection(
        threadState,
        cancelCurrentLoads = {
            loadJob?.cancel()
            olderHistory.cancel()
            observeJob?.cancel()
            observeJob = null
        },
        resetPage = messagePageLoader::reset,
        isDestinationActive = { destinationLifecycle.isActive },
        reload = ::reloadSelectedConversation,
        observe = ::observeConversation
    )
    private val streamCallbacks = MessagesStreamCallbacksFactory(
        threadState,
        inbox,
        ::reloadSelectedConversation,
        showFailure,
        { !messagePageLoader.initialPageLoaded },
        readReceiptHandler::markIfNeeded
    )

    init { viewModelScope.launch { inbox.state.collect(::applyInboxSelection) } }

    fun loadConversations() {
        inbox.refresh()
        applyInboxSelection(inbox.state.value)
    }

    fun onDestinationActiveChanged(isActive: Boolean) {
        destinationLifecycle.onActiveChanged(isActive)
    }

    private fun applyInboxSelection(snapshot: InboxState) {
        if (snapshot.viewerId != visibleViewerId) {
            val previousViewerId = visibleViewerId
            visibleViewerId = snapshot.viewerId
            loadJob?.cancel()
            olderHistory.cancel()
            observeJob?.cancel()
            observeJob = null
            listingPreviewLoader.cancel()
            requestedConversationId = null
            streamCallbacks.clearPending()
            messagePageLoader.reset()
            threadState.value = MessagesUiState()
            if (previousViewerId != null && snapshot.viewerId != null) {
                threadState.update { it.copy(errorMessage = "Conversation is unavailable for this user.") }
            }
        }
        if (snapshot.viewerId == null) return
        if (destinationLifecycle.isActive) listingPreviewLoader.load(snapshot.conversations)
        if (snapshot.isLoading) return
        val current = threadState.value.selectedConversationId
        if (streamCallbacks.isWaitingForInbox(snapshot, current, requestedConversationId)) {
            requestedConversationId = current ?: requestedConversationId
            if (current != null) conversationSelection.select(null)
            return
        }
        val selected = snapshot.conversations.firstOrNull { it.id == requestedConversationId }
            ?: snapshot.conversations.firstOrNull { it.id == current }
            ?: snapshot.conversations.firstOrNull()
        if (selected?.id == requestedConversationId) requestedConversationId = null
        if (selected?.id != current) conversationSelection.select(selected?.id)
        streamCallbacks.onInboxSnapshot(snapshot)
    }

    fun selectConversation(conversationId: ConversationId) {
        if (inbox.state.value.conversations.none { it.id == conversationId }) return
        requestedConversationId = null
        streamCallbacks.clearPending()
        threadState.update { it.copy(isConversationOpen = true) }
        conversationSelection.select(conversationId)
    }

    fun returnToInbox() {
        editing.cancel()
        threadState.update { it.copy(isConversationOpen = false) }
    }

    val openConversation: (ConversationId) -> Unit = { conversationId ->
        streamCallbacks.clearPending()
        requestedConversationId = conversationId
        threadState.update { it.copy(isConversationOpen = true) }
        conversationSelection.select(null)
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
        loadJob = viewModelScope.launch { messagePageLoader.load(conversationId) }
    }

    fun loadOlderMessages() = olderHistory.load()
    fun hideConversationFromInbox(conversationId: ConversationId) =
        actions.hideConversation(conversationId, showFailure)

    fun onMessageInputChanged(input: String) {
        threadState.update {
            it.copy(
                messageInput = input,
                editError = null,
                errorMessage = null,
                statusMessage = null
            )
        }
    }

    val toggleReportedMessageVisibility: (MessageId) -> Unit = actions::toggleVisibility

    fun edit(messageId: MessageId) {
        inbox.synchronizeIdentity()
        applyInboxSelection(inbox.state.value)
        editing.begin(messageId)
    }

    val cancelEditing: () -> Unit = editing::cancel

    fun sendCurrentMessage() {
        inbox.synchronizeIdentity()
        applyInboxSelection(inbox.state.value)
        if (threadState.value.editState is MessageEditState.Editing) editing.save() else actions.send(showFailure)
    }

    val report: (MessageId) -> Unit = messageReportActions::begin
    val selectMessageReportReason: (ReportReason) -> Unit = messageReportActions::selectReason
    val updateMessageReportDescription: (String) -> Unit = messageReportActions::updateDescription
    val dismissMessageReport: () -> Unit = messageReportActions::dismiss
    val submitMessageReport: () -> Unit = { messageReportActions.submit(::applyInboxSelection) }
}
