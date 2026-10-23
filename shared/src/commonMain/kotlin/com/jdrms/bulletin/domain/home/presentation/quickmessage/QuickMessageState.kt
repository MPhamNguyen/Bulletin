package com.jdrms.bulletin.domain.home.presentation.quickmessage

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

internal enum class SendPhase { Idle, Sending, Sent }

internal data class QuickMessageActions(
    val onSend: suspend (String) -> Boolean,
    val onSending: () -> Unit,
    val onSendFailed: () -> Unit,
    val onAnimationComplete: () -> Unit
)

internal class QuickMessageState(
    private val scope: CoroutineScope,
    private val draft: MutableState<String>
) {
    var message: String
        get() = draft.value
        private set(value) { draft.value = value }

    var phase by mutableStateOf(SendPhase.Idle)
        private set
    var sentMessage by mutableStateOf<String?>(null)
        private set
    var sendFailed by mutableStateOf(false)
        private set

    private var onSend: suspend (String) -> Boolean = { false }
    private var onSending: () -> Unit = {}
    private var onSendFailed: () -> Unit = {}
    private var onAnimationComplete: () -> Unit = {}

    private val arrowAnimation = Animatable(0f)
    private val bubbleAnimation = Animatable(0f)
    private val receiptAnimation = Animatable(0f)

    val arrowProgress: State<Float> = arrowAnimation.asState()
    val bubbleProgress: State<Float> = bubbleAnimation.asState()
    val receiptProgress: State<Float> = receiptAnimation.asState()

    fun updateCallbacks(actions: QuickMessageActions) {
        onSend = actions.onSend
        onSending = actions.onSending
        onSendFailed = actions.onSendFailed
        onAnimationComplete = actions.onAnimationComplete
    }

    fun onMessageChange(value: String) {
        if (phase != SendPhase.Idle) return
        message = value.take(MAX_MESSAGE_LENGTH)
        sendFailed = false
    }

    fun onReplyChosen(reply: String) {
        if (phase != SendPhase.Idle) return
        message = reply
        sendFailed = false
    }

    fun submit() {
        val content = message.trim()
        if (content.isEmpty() || phase != SendPhase.Idle) return
        sendFailed = false
        phase = SendPhase.Sending
        scope.launch { send(content) }
    }

    private suspend fun send(content: String) {
        onSending()
        val arrowJob = scope.launch {
            arrowAnimation.animateTo(1f, tween(QuickMessageTiming.arrowDurationMs))
        }
        val result = runCatching {
            onSend(content)
        }
        val delivered = result.getOrElse { failure ->
            if (failure is CancellationException) throw failure
            false
        }

        if (!delivered) {
            recoverFromFailure(arrowJob)
            return
        }

        showConfirmation(content, arrowJob)
    }

    private suspend fun recoverFromFailure(arrowJob: Job) {
        arrowJob.cancel()
        arrowAnimation.animateTo(0f, tween(QuickMessageTiming.failureReturnDurationMs))
        sendFailed = true
        phase = SendPhase.Idle
        onSendFailed()
    }

    private suspend fun showConfirmation(content: String, arrowJob: Job) {
        message = ""
        sentMessage = content
        phase = SendPhase.Sent
        arrowJob.join()
        delay(QuickMessageTiming.bubbleDelayMs)
        bubbleAnimation.animateTo(
            1f,
            spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow)
        )
        delay(QuickMessageTiming.receiptDelayMs)
        receiptAnimation.animateTo(1f, tween(QuickMessageTiming.receiptDurationMs))
        delay(QuickMessageTiming.confirmationHoldMs)
        onAnimationComplete()
    }

    private companion object {
        const val MAX_MESSAGE_LENGTH = 200
    }
}

@Composable
internal fun rememberQuickMessageState(
    listingId: String,
    actions: QuickMessageActions
): QuickMessageState {
    val draft = rememberSaveable(listingId) { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val state = remember(listingId, scope, draft) { QuickMessageState(scope, draft) }
    state.updateCallbacks(actions)
    return state
}
