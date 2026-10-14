package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.GetConversationMessages
import com.jdrms.bulletin.domain.messages.domain.service.MessageTimeline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
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
    }

    fun load() {
        val selected = state.value.selectedConversationId ?: return
        val cursor = state.value.olderCursor ?: return
        if (job?.isActive == true) return
        state.update { it.copy(isLoadingOlder = true) }
        job = scope.launch {
            when (val result = getConversationMessages(selected, cursor)) {
                is Result.Success -> state.update {
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
}
