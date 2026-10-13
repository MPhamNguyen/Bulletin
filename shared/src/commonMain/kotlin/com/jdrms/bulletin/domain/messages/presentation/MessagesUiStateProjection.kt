package com.jdrms.bulletin.domain.messages.presentation

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** A rendered projection; inbox conversations are stored only by [InboxStateOwner]. */
@OptIn(ExperimentalForInheritanceCoroutinesApi::class)
internal class MessagesUiStateProjection(
    private val thread: StateFlow<MessagesUiState>,
    private val inbox: StateFlow<InboxState>,
    scope: CoroutineScope
) : StateFlow<MessagesUiState> {
    private val changes = combine(thread, inbox, ::withInbox)
        .stateIn(scope, SharingStarted.Eagerly, withInbox(thread.value, inbox.value))

    override val value: MessagesUiState get() = withInbox(thread.value, inbox.value)
    override val replayCache: List<MessagesUiState> get() = listOf(value)

    override suspend fun collect(collector: FlowCollector<MessagesUiState>): Nothing = changes.collect(collector)
}

private fun withInbox(thread: MessagesUiState, inbox: InboxState): MessagesUiState = thread.copy(
    viewerId = inbox.viewerId,
    conversations = inbox.conversations,
    isLoading = inbox.isLoading,
    errorMessage = thread.errorMessage ?: inbox.errorMessage
)
