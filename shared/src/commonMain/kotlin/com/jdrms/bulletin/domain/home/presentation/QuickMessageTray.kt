package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sqrt

internal val QUICK_MESSAGE_TRAY_HEIGHT = 164.dp

private val QuickReplies = listOf(
    "Is this still available?",
    "Can you do $500?",
    "Where can we meet?",
    "Can I see more photos?"
)

@Composable
internal fun QuickMessageTray(
    listingId: String,
    visibleProgress: Float,
    onSend: suspend (String) -> Boolean,
    onSending: () -> Unit,
    onSendFailed: () -> Unit,
    onAnimationComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    var message by remember(listingId) { mutableStateOf("") }
    var sentMessage by remember(listingId) { mutableStateOf<String?>(null) }
    var sendFailed by remember(listingId) { mutableStateOf(false) }
    var confirmationStarted by remember(listingId) { mutableStateOf(false) }
    var isSending by remember(listingId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val reveal = ((visibleProgress - 0.3f) * 1.5f).coerceIn(0f, 1f)
    val launchProgress = remember(listingId) { Animatable(0f) }
    val bubbleProgress = remember(listingId) { Animatable(0f) }
    val receiptProgress = remember(listingId) { Animatable(0f) }

    fun send() {
        val content = message.trim()
        if (content.isEmpty() || isSending || confirmationStarted) return
        isSending = true
        onSending()
        scope.launch {
            launch { launchProgress.animateTo(1f, tween(450)) }
            if (!onSend(content)) {
                isSending = false
                sendFailed = true
                launchProgress.snapTo(0f)
                onSendFailed()
                return@launch
            }

            isSending = false
            sendFailed = false
            message = ""
            confirmationStarted = true
            delay(100)
            sentMessage = content
            bubbleProgress.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = 500
                    0.98f at 330
                    1.08f at 420
                    1f at 500
                }
            )
            delay(90)
            receiptProgress.animateTo(1f, tween(200))
            delay(10)
            onAnimationComplete()
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(QUICK_MESSAGE_TRAY_HEIGHT)
            .graphicsLayer {
                alpha = reveal
                translationY = (1f - visibleProgress) * 28.dp.toPx()
            }
            .padding(start = 12.dp, end = 12.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        when {
            !confirmationStarted && sendFailed -> Text(
                "Couldn't send. Try again.",
                modifier = Modifier.padding(bottom = 8.dp),
                color = Color(0xFFFFB4AB),
                style = MaterialTheme.typography.labelMedium
            )
            !confirmationStarted -> QuickReplyRow(onReply = { message = it; sendFailed = false })
            sentMessage == null -> Spacer(Modifier.height(76.dp).padding(bottom = 8.dp))
            else -> SentMessageConfirmation(
                message = sentMessage.orEmpty(),
                bubbleProgress = bubbleProgress.value,
                receiptProgress = receiptProgress.value
            )
        }
        MessageComposer(
            message = message,
            enabled = !isSending && !confirmationStarted,
            arrowProgress = launchProgress.value,
            onMessageChange = { message = it.take(200); sendFailed = false },
            onSend = ::send
        )
    }
}

@Composable
private fun QuickReplyRow(onReply: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        QuickReplies.forEach { reply ->
            Surface(
                onClick = { onReply(reply) },
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.08f),
                contentColor = Color(0xFFE6EDF7),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
            ) {
                Text(
                    reply,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun SentMessageConfirmation(message: String, bubbleProgress: Float, receiptProgress: Float) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(0.8f).graphicsLayer {
                alpha = bubbleProgress.coerceIn(0f, 1f)
                scaleX = 0.6f + bubbleProgress * 0.4f
                scaleY = 0.6f + bubbleProgress * 0.4f
                translationX = (1f - bubbleProgress) * 10.dp.toPx()
                translationY = (1f - bubbleProgress) * 52.dp.toPx()
                transformOrigin = TransformOrigin(1f, 1f)
            },
            shape = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp),
            color = Color(0xFFCFE0F5)
        ) {
            Text(
                message,
                modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
                color = Color(0xFF0F1A2B),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(
            modifier = Modifier.graphicsLayer { alpha = receiptProgress },
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DrawnCheckmark(receiptProgress)
            Text("Sent", color = Color(0xFF9FD8CF), style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun DrawnCheckmark(progress: Float) {
    Canvas(Modifier.size(14.dp)) {
        val start = Offset(size.width * 0.15f, size.height * 0.52f)
        val corner = Offset(size.width * 0.42f, size.height * 0.8f)
        val end = Offset(size.width * 0.88f, size.height * 0.18f)
        val stroke = 2.dp.toPx()
        if (progress > 0f) {
            val firstProgress = (progress / 0.34f).coerceIn(0f, 1f)
            drawLine(
                Color(0xFF9FD8CF), start,
                Offset(start.x + (corner.x - start.x) * firstProgress, start.y + (corner.y - start.y) * firstProgress),
                stroke, cap = StrokeCap.Round
            )
        }
        if (progress > 0.34f) {
            val secondProgress = ((progress - 0.34f) / 0.66f).coerceIn(0f, 1f)
            drawLine(
                Color(0xFF9FD8CF), corner,
                Offset(corner.x + (end.x - corner.x) * secondProgress, corner.y + (end.y - corner.y) * secondProgress),
                stroke, cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun MessageComposer(
    message: String,
    enabled: Boolean,
    arrowProgress: Float,
    onMessageChange: (String) -> Unit,
    onSend: () -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth().height(54.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.fillMaxWidth().height(54.dp),
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.07f),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
        ) {
            Row(
                modifier = Modifier.padding(start = 18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BasicTextField(
                    value = message,
                    onValueChange = onMessageChange,
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { onSend() }),
                    decorationBox = { inner ->
                        Box {
                            if (message.isEmpty()) Text("Write a message…", color = Color(0xFF8E9DB3))
                            inner()
                        }
                    }
                )
                Surface(
                    onClick = onSend,
                    enabled = enabled && message.isNotBlank(),
                    shape = CircleShape,
                    color = Color(0xFFCFE0F5).copy(alpha = if (enabled && message.isNotBlank()) 1f else 0.3f),
                    contentColor = Color(0xFF0F1A2B),
                    modifier = Modifier.size(44.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            "↑",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.graphicsLayer {
                                translationY = arrowProgress * -26.dp.toPx()
                                alpha = 1f - arrowProgress
                            }
                        )
                    }
                }
            }
        }
    }
}
