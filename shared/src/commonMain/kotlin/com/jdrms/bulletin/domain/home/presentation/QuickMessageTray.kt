package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var message by remember(listingId) { mutableStateOf("") }
    var sentMessage by remember(listingId) { mutableStateOf<String?>(null) }
    var sendFailed by remember(listingId) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val reveal = ((visibleProgress - 0.3f) * 1.5f).coerceIn(0f, 1f)
    fun send() {
        val content = message.trim()
        if (content.isEmpty() || sentMessage != null) return
        scope.launch {
            if (onSend(content)) {
                sentMessage = content
                sendFailed = false
                message = ""
                delay(1_700)
                sentMessage = null
                onClose()
            } else {
                sendFailed = true
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .graphicsLayer {
                alpha = reveal
                translationY = (1f - visibleProgress) * 28.dp.toPx()
            }
            .padding(start = 12.dp, end = 12.dp, bottom = 14.dp),
        verticalArrangement = Arrangement.Bottom
    ) {
        if (sentMessage == null) {
            if (sendFailed) {
                Text(
                    "Couldn't send. Try again.",
                    modifier = Modifier.padding(bottom = 8.dp),
                    color = Color(0xFFFFB4AB),
                    style = MaterialTheme.typography.labelMedium
                )
            } else Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickReplies.forEach { reply ->
                    Surface(
                        onClick = { message = reply; sendFailed = false },
                        shape = CircleShape,
                        color = Color.White.copy(alpha = 0.08f),
                        contentColor = Color(0xFFE6EDF7),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text(reply, modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            style = MaterialTheme.typography.labelLarge, maxLines = 1)
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth().height(48.dp).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("✓ Sent", color = Color(0xFF9FD8CF), style = MaterialTheme.typography.labelMedium)
                Surface(shape = RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp), color = Color(0xFFCFE0F5)) {
                    Text(sentMessage.orEmpty(), modifier = Modifier.padding(horizontal = 15.dp, vertical = 10.dp),
                        color = Color(0xFF0F1A2B), maxLines = 1, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().height(54.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = CircleShape,
                color = Color.White.copy(alpha = 0.07f),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
            ) {
                Row(
                    modifier = Modifier.padding(start = 18.dp, end = 5.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BasicTextField(
                        value = message,
                        onValueChange = { message = it.take(200); sendFailed = false },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Color.White),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                        keyboardActions = KeyboardActions(onSend = { send() }),
                        decorationBox = { inner ->
                            Box {
                                if (message.isEmpty()) Text("Write a message…", color = Color(0xFF8E9DB3))
                                inner()
                            }
                        }
                    )
                    Surface(
                        onClick = { send() },
                        enabled = message.isNotBlank() && sentMessage == null,
                        shape = CircleShape,
                        color = Color(0xFFCFE0F5).copy(alpha = if (message.isNotBlank() && sentMessage == null) 1f else 0.3f),
                        contentColor = Color(0xFF0F1A2B),
                        modifier = Modifier.height(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 14.dp)) {
                            Text("↑", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }
    }

}
