package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.GetConversationMessages
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/** Coordinates history jobs without replacing the selected conversation or its composer. */
internal class MessageHistoryInteraction(
    private val state: MutableStateFlow<MessagesUiState>,
    inbox: InboxStateOwner,
    getConversationMessages: GetConversationMessages,
    readReceiptHandler: ConversationReadReceiptHandler,
    private val scope: CoroutineScope,
    showFailure: (Result.Error) -> Unit
) {
    private var loadJob: Job? = null
    private val pages = ConversationMessagePageLoader(
        state,
        inbox,
        getConversationMessages,
        readReceiptHandler,
        showFailure
    )
    private val older = MessagesOlderHistoryLoader(state, getConversationMessages, scope, showFailure)
    val initialPageLoaded: Boolean get() = pages.initialPageLoaded

    fun reload(conversationId: ConversationId) {
        loadJob?.cancel()
        loadJob = scope.launch { pages.load(conversationId) }
    }

    fun loadOlder() = older.load()

    fun retry(request: MessageHistoryRequest) {
        if (state.value.selectedConversationId != request.conversationId) return
        when (request) {
            is MessageHistoryRequest.Thread -> {
                if (state.value.failedHistoryRequest != request || loadJob?.isActive == true) return
                loadJob = scope.launch { pages.load(request) }
            }
            is MessageHistoryRequest.Older -> older.retry(request)
        }
    }

    fun cancel() {
        loadJob?.cancel()
        loadJob = null
        older.cancel()
    }

    fun reset() = pages.reset()
}
