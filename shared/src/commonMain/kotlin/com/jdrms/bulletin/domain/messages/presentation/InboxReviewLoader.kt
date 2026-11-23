package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.GetConversations
import com.jdrms.bulletin.domain.messages.application.MessageInbox
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class InboxReviewLoader(
    private val getConversations: GetConversations,
    private val mutableState: MutableStateFlow<InboxState>,
    private val scope: CoroutineScope
) {
    private var job: Job? = null

    fun refresh(userId: SenderId) {
        job?.cancel()
        job = scope.launch {
            val snapshot = mutableState.value
            val result = getConversations.enrich(
                MessageInbox(userId, snapshot.conversations)
            )
            if (mutableState.value.viewerId != userId || result !is Result.Success) return@launch
            mutableState.update {
                it.copy(
                    pendingReviewConversationIds = result.data.pendingReviewConversationIds
                        ?: it.pendingReviewConversationIds,
                    unmatchedReviewRequests = result.data.unmatchedReviewRequests ?: it.unmatchedReviewRequests
                )
            }
        }
    }

    fun cancel() { job?.cancel() }
}
