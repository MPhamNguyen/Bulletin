package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.ConversationUserBlocker
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Owns confirmation, persistence, and status reads for the current viewer's explicit blocks. */
internal class ConversationBlockActions(
    private val state: MutableStateFlow<MessagesUiState>,
    private val blocker: ConversationUserBlocker?,
    private val scope: CoroutineScope
) {
    private var generation = 0
    private var statusRevision = 0
    private val jobs = mutableListOf<Job>()

    fun reset() {
        generation++
        jobs.forEach { it.cancel() }
        jobs.clear()
    }

    fun request(target: BlockTarget) {
        val current = state.value.block
        if (current.flow is ConversationBlockFlow.Submitting || current.isBlocked(target.userId)) return
        update { it.copy(flow = ConversationBlockFlow.Confirming(target), unblockErrorMessage = null) }
    }

    fun dismiss() {
        if (state.value.block.flow !is ConversationBlockFlow.Submitting) {
            update { it.copy(flow = ConversationBlockFlow.Idle) }
        }
    }

    fun confirm() {
        val flow = state.value.block.flow as? ConversationBlockFlow.Confirming ?: return
        submit(flow.target, BlockAction.BLOCK)
    }

    fun unblock(target: BlockTarget) {
        val current = state.value.block
        if (current.flow is ConversationBlockFlow.Submitting || !current.isBlocked(target.userId)) return
        submit(target, BlockAction.UNBLOCK)
    }

    fun refreshStatus(userId: SenderId) {
        val port = blocker ?: return
        if (state.value.block.flow is ConversationBlockFlow.Submitting) return
        val requestGeneration = generation
        val revision = ++statusRevision
        update { it.copy(checkedUserIds = it.checkedUserIds - userId, statusErrorMessage = null) }
        jobs.removeAll { it.isCompleted }
        jobs += scope.launch {
            val result = attempt { port.isBlocked(userId) }
            if (requestGeneration != generation || revision != statusRevision) return@launch
            update { current ->
                when (result) {
                    is Result.Success -> current.copy(
                        blockedUserIds = if (result.data) {
                            current.blockedUserIds + userId
                        } else {
                            current.blockedUserIds - userId
                        },
                        checkedUserIds = current.checkedUserIds + userId
                    )
                    is Result.Error -> current.copy(statusErrorMessage = result.message)
                }
            }
        }
    }

    private fun submit(target: BlockTarget, action: BlockAction) {
        val port = blocker
        if (port == null) {
            fail(target, action, "Blocking is unavailable right now.")
            return
        }
        val requestGeneration = generation
        statusRevision++
        update { it.copy(flow = ConversationBlockFlow.Submitting(target, action), unblockErrorMessage = null) }
        jobs.removeAll { it.isCompleted }
        jobs += scope.launch {
            val result = attempt {
                if (action == BlockAction.BLOCK) port.block(target.userId) else port.unblock(target.userId)
            }
            if (requestGeneration != generation) return@launch
            when (result) {
                is Result.Success -> update {
                    it.copy(
                        blockedUserIds = if (action == BlockAction.BLOCK) {
                            it.blockedUserIds + target.userId
                        } else {
                            it.blockedUserIds - target.userId
                        },
                        checkedUserIds = it.checkedUserIds + target.userId,
                        flow = ConversationBlockFlow.Idle,
                        statusErrorMessage = null
                    )
                }
                is Result.Error -> fail(target, action, result.message)
            }
        }
    }

    private fun fail(target: BlockTarget, action: BlockAction, message: String) = update {
        when (action) {
            BlockAction.BLOCK -> it.copy(flow = ConversationBlockFlow.Confirming(target, message))
            BlockAction.UNBLOCK -> it.copy(flow = ConversationBlockFlow.Idle, unblockErrorMessage = message)
        }
    }

    private fun update(transform: (ConversationBlockUiState) -> ConversationBlockUiState) {
        state.update { it.copy(block = transform(it.block)) }
    }

    // Unexpected port failures must become a retryable UI error; cancellation still propagates.
    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> attempt(call: suspend () -> Result<T>): Result<T> = try {
        call()
    } catch (exception: CancellationException) {
        throw exception
    } catch (exception: Exception) {
        Result.Error(exception, "Something went wrong. Please try again.")
    }
}
