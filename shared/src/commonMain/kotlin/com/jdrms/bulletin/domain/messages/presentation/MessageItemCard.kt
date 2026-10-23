package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.jdrms.bulletin.domain.messages.domain.model.Message

@Composable
internal fun MessageItemCard(
    message: Message,
    maxBubbleWidth: Dp,
    isMine: Boolean,
    showTimestamp: Boolean,
    showSeenReceipt: Boolean = false,
    showAvatar: Boolean,
    isRevealed: Boolean,
    canReport: Boolean,
    conversationTitle: String,
    onReport: () -> Unit,
    onToggleVisibility: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalArrangement = if (isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!isMine) {
            if (showAvatar) ConversationAvatar(conversationTitle, Modifier)
            else Spacer(Modifier.width(40.dp))
            Spacer(Modifier.width(8.dp))
        }
        Column(horizontalAlignment = if (isMine) Alignment.End else Alignment.Start) {
            var reportMenuExpanded by remember(message.id) { mutableStateOf(false) }
            val canReportMessage = !isMine && canReport && !message.isReported
            androidx.compose.foundation.layout.Box {
                Surface(
                    color = when {
                        message.isReported -> MaterialTheme.colorScheme.errorContainer
                        isMine -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    },
                    contentColor = when {
                        message.isReported -> MaterialTheme.colorScheme.onErrorContainer
                        isMine -> MaterialTheme.colorScheme.onPrimary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    shape = RoundedCornerShape(
                        topStart = 18.dp,
                        topEnd = 18.dp,
                        bottomEnd = if (isMine && showTimestamp) 4.dp else 18.dp,
                        bottomStart = if (!isMine && showTimestamp) 4.dp else 18.dp
                    ),
                    modifier = Modifier
                        .widthIn(max = maxBubbleWidth)
                        .then(
                            if (canReportMessage) Modifier.combinedClickable(
                                onClick = {},
                                onLongClick = { reportMenuExpanded = true }
                            ) else Modifier
                        )
                ) {
                    Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                        Text(messageBodyText(message, isRevealed), style = MaterialTheme.typography.bodyMedium)
                        if (message.isReported) {
                            Text("Reported for moderation", style = MaterialTheme.typography.labelSmall)
                            TextButton(onClick = onToggleVisibility, contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)) {
                                Text(if (isRevealed) "Hide message" else "Show message", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
                DropdownMenu(
                    expanded = reportMenuExpanded,
                    onDismissRequest = { reportMenuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Report message") },
                        onClick = {
                            reportMenuExpanded = false
                            if (canReportMessage) onReport()
                        },
                        enabled = canReportMessage
                    )
                }
            }
            if (showSeenReceipt) {
                Text(
                    "Seen",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        if (isMine) Spacer(Modifier.width(40.dp))
    }
}
