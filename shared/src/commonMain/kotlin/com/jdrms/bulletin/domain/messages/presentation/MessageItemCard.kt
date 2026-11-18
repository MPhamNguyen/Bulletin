package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageEditing
import kotlin.time.Clock

@Composable
internal fun MessageItemCard(
    message: Message,
    presentation: MessageItemCardPresentation,
    actions: MessageItemCardActions
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalArrangement = if (presentation.isMine) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Bottom
    ) {
        if (!presentation.isMine) MessageSenderAvatar(message, presentation.showAvatar, actions.onViewSenderProfile)
        Column(horizontalAlignment = bubbleAlignment(presentation.isMine)) {
            if (message.editedAt != null) {
                Text(
                    "Edited",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp, bottom = 6.dp)
                )
            }
            MessageBubble(message, presentation, actions)
            if (presentation.showSeenReceipt) SeenReceipt()
        }
    }
}

private fun bubbleAlignment(isMine: Boolean): Alignment.Horizontal =
    if (isMine) Alignment.End else Alignment.Start

@Composable
private fun MessageBubble(
    message: Message,
    presentation: MessageItemCardPresentation,
    actions: MessageItemCardActions
) {
    var reportMenuExpanded by remember(message.id) { mutableStateOf(false) }
    val canEdit = !actions.editingDisabled && MessageEditing.canEdit(
        message, message.senderId.takeIf { presentation.isMine }, Clock.System.now()
    )
    val canReportMessage = !presentation.isMine && presentation.canReport && !message.isReported
    Box {
        Surface(
            color = messageBubbleColor(message, presentation.isMine),
            contentColor = messageBubbleContentColor(message, presentation.isMine),
            shape = messageBubbleShape(presentation),
            modifier = Modifier
                .widthIn(max = presentation.maxBubbleWidth)
                .then(reportableBubbleModifier(canReportMessage || canEdit) { reportMenuExpanded = true })
        ) {
            MessageBubbleContent(message, presentation.isRevealed, actions.onToggleVisibility)
        }
        MessageReportMenu(
            expanded = reportMenuExpanded,
            canReport = canReportMessage,
            canEdit = canEdit,
            onEdit = {
                reportMenuExpanded = false
                actions.onEdit()
            },
            onDismiss = { reportMenuExpanded = false },
            onReport = {
                reportMenuExpanded = false
                actions.onReport()
            }
        )
    }
}

@Composable
private fun MessageBubbleContent(
    message: Message,
    isRevealed: Boolean,
    onToggleVisibility: () -> Unit
) {
    Column(Modifier.padding(horizontal = 10.dp, vertical = BulletinSpacing.s)) {
        Text(messageBodyText(message, isRevealed), style = MaterialTheme.typography.bodyMedium)
        if (message.isReported) {
            Text("Reported for moderation", style = MaterialTheme.typography.labelSmall)
            BulletinButton(
                onClick = onToggleVisibility,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                variant = BulletinButtonVariant.Text
            ) {
                Text(
                    if (isRevealed) "Hide message" else "Show message",
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun MessageReportMenu(
    expanded: Boolean,
    canReport: Boolean,
    canEdit: Boolean,
    onEdit: () -> Unit,
    onDismiss: () -> Unit,
    onReport: () -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        if (canEdit) DropdownMenuItem(text = { Text("Edit message") }, onClick = onEdit)
        if (canReport) DropdownMenuItem(text = { Text("Report message") }, onClick = onReport)
    }
}

@Composable
private fun SeenReceipt() {
    Text(
        "Seen",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun messageBubbleColor(message: Message, isMine: Boolean) = when {
    message.isReported -> MaterialTheme.colorScheme.errorContainer
    isMine -> MaterialTheme.colorScheme.primary
    else -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun messageBubbleContentColor(message: Message, isMine: Boolean) = when {
    message.isReported -> MaterialTheme.colorScheme.onErrorContainer
    isMine -> MaterialTheme.colorScheme.onPrimary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun messageBubbleShape(presentation: MessageItemCardPresentation) = BulletinShapes.bubble(
    squareBottomStart = !presentation.isMine && presentation.showTimestamp,
    squareBottomEnd = presentation.isMine && presentation.showTimestamp
)

private fun reportableBubbleModifier(canReport: Boolean, onLongClick: () -> Unit): Modifier =
    if (canReport) Modifier.combinedClickable(onClick = {}, onLongClick = onLongClick) else Modifier
