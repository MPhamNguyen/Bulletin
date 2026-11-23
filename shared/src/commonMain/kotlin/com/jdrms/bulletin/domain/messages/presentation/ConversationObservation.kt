package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.application.ObserveConversationMessages
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

/** Owns the selected conversation's live subscription. */
internal class ConversationObservation(
    observeMessages: ObserveConversationMessages,
    private val state: MutableStateFlow<MessagesUiState>,
    private val callbacks: MessagesStreamCallbacksFactory,
    private val scope: CoroutineScope
) {
    private val coordinator = MessageStreamCoordinator(observeMessages)
    private var job: Job? = null

    fun observe(conversationId: ConversationId) {
        cancel()
        state.update { it.copy(connectionState = MessageConnectionState.CONNECTING) }
        job = coordinator.observe(scope, conversationId, callbacks.forConversation(conversationId))
    }

    fun cancel() {
        job?.cancel()
        job = null
    }
}
