package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.formatRelativeTime
import com.jdrms.bulletin.domain.messages.domain.model.Message

@Composable
internal fun MessageItemCard(
    message: Message,
    isRevealed: Boolean,
    canReport: Boolean,
    onReport: () -> Unit,
    onToggleVisibility: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (message.isReported) {
                MaterialTheme.colorScheme.errorContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            contentColor = if (message.isReported) {
                MaterialTheme.colorScheme.onErrorContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            MessageMetadataText(message.senderName, message.isReported, FontWeight.Bold)
            Text(
                text = messageBodyText(message, isRevealed),
                style = MaterialTheme.typography.bodyMedium,
                color = if (message.isReported) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            MessageMetadataText(
                formatRelativeTime(message.timestampMillis, currentTimeMillis()),
                message.isReported,
                null
            )
            if (message.isReported) {
                Text(
                    text = "[Reported for moderation]",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
                TextButton(onClick = onToggleVisibility) {
                    Text(if (isRevealed) "Hide message" else "Show message")
                }
            } else {
                TextButton(
                    onClick = onReport,
                    enabled = canReport,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Report", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
private fun MessageMetadataText(text: String, isReported: Boolean, fontWeight: FontWeight?) {
    val color = if (isReported) {
        MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = fontWeight,
        color = color
    )
}
