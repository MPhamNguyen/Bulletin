package com.jdrms.bulletin.domain.home.presentation.quickmessage

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** Quick contact surface attached to a listing card. */
@Composable
internal fun QuickMessageTray(
    listingId: String,
    visibleProgress: Float,
    actions: QuickMessageActions,
    modifier: Modifier = Modifier,
    suggestedOffer: String? = null,
    quickSendTrigger: Int = 0
) {
    val state = rememberQuickMessageState(listingId, actions)
    val replies = remember(suggestedOffer) { quickMessageReplies(suggestedOffer) }
    val reveal = ((visibleProgress - 0.3f) * 1.5f).coerceIn(0f, 1f)
    val visible = reveal > 0f
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(quickSendTrigger) {
        if (quickSendTrigger > 0) state.submitQuickReply(DEFAULT_QUICK_REPLY)
    }
    LaunchedEffect(visible) { if (!visible) focusManager.clearFocus() }
    DisposableEffect(focusManager) { onDispose { focusManager.clearFocus() } }

    Column(
        modifier = trayModifier(modifier, visibleProgress, reveal),
        verticalArrangement = Arrangement.Bottom
    ) {
        QuickMessageContents(
            visible = visible,
            state = state,
            replies = replies,
            focusRequester = focusRequester,
            onSend = {
                focusManager.clearFocus()
                state.submit()
            }
        )
    }
}

private fun trayModifier(modifier: Modifier, progress: Float, reveal: Float) = modifier
    .fillMaxWidth()
    .height(QUICK_MESSAGE_TRAY_HEIGHT)
    .graphicsLayer {
        alpha = reveal
        translationY = (1f - progress) * 28.dp.toPx()
    }
    .padding(start = 12.dp, end = 12.dp, bottom = 14.dp)

@Composable
private fun QuickMessageContents(
    visible: Boolean,
    state: QuickMessageState,
    replies: List<String>,
    focusRequester: FocusRequester,
    onSend: () -> Unit
) {
    if (!visible) return
    Crossfade(targetState = state.sentMessage != null, animationSpec = tween(150)) { showSent ->
        if (showSent) {
            SentConfirmation(state.sentMessage.orEmpty(), state.bubbleProgress, state.receiptProgress)
        } else {
            QuickReplyContent(state, replies, focusRequester)
        }
    }
    MessageComposer(
        message = state.message,
        editable = state.phase == SendPhase.Idle,
        sending = state.phase == SendPhase.Sending,
        arrowProgress = state.arrowProgress,
        focusRequester = focusRequester,
        onMessageChange = state::onMessageChange,
        onSend = onSend
    )
}

@Composable
private fun QuickReplyContent(state: QuickMessageState, replies: List<String>, focusRequester: FocusRequester) {
    Column {
        if (state.sendFailed) SendFailureMessage()
        QuickReplies(replies) {
            state.onReplyChosen(it)
            runCatching { focusRequester.requestFocus() }
        }
    }
}

@Composable
private fun SendFailureMessage() {
    Text(
        "Couldn't send. Try again.",
        modifier = Modifier
            .padding(start = 6.dp, bottom = 6.dp)
            .semantics { liveRegion = LiveRegionMode.Polite },
        color = QuickMessageTokens.error,
        style = MaterialTheme.typography.labelMedium
    )
}
