package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.GetConversationMessages
import com.jdrms.bulletin.domain.messages.domain.service.MessageTimeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class MessagesOlderHistoryLoader(
    private val state: MutableStateFlow<MessagesUiState>,
    private val getConversationMessages: GetConversationMessages,
    private val scope: CoroutineScope,
    private val showFailure: (Result.Error) -> Unit
) {
    private var job: Job? = null

    fun cancel() {
        job?.cancel()
        job = null
    }

    fun load() {
        if (state.value.failedOlderHistoryRequest != null) return
        val selected = state.value.selectedConversationId ?: return
        val cursor = state.value.olderCursor ?: return
        load(MessageHistoryRequest.Older(selected, cursor))
    }

    fun retry(request: MessageHistoryRequest.Older) {
        if (state.value.failedOlderHistoryRequest != request || state.value.olderCursor != request.before) return
        load(request)
    }

    private fun load(request: MessageHistoryRequest.Older) {
        if (job?.isActive == true) return
        state.update { it.copy(isLoadingOlder = true) }
        job = scope.launch {
            val result = getConversationMessages(request.conversationId, request.before)
            currentCoroutineContext().ensureActive()
            if (state.value.selectedConversationId != request.conversationId ||
                state.value.olderCursor != request.before
            ) {
                return@launch
            }
            when (result) {
                is Result.Success -> state.update {
                    it.copy(
                        currentMessages = MessageTimeline.merge(it.currentMessages, result.data.messages),
                        olderCursor = result.data.olderCursor,
                        isLoadingOlder = false,
                        failedOlderHistoryRequest = null
                    )
                }
                is Result.Error -> fail(request, result)
            }
        }
    }

    private fun fail(request: MessageHistoryRequest.Older, error: Result.Error) {
        if (error.isMessagingIdentityUnavailable) {
            showFailure(error)
        } else {
            state.update { it.copy(isLoadingOlder = false, failedOlderHistoryRequest = request) }
        }
    }
}
