package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.formatRelativeTime
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

@Composable
internal fun ConversationSummary(
    conversation: Conversation,
    viewerId: SenderId?,
    titleColor: androidx.compose.ui.graphics.Color,
    secondaryColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            conversationTitle(conversation, viewerId),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = titleColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            conversationPreviewText(conversation),
            style = MaterialTheme.typography.bodyMedium,
            color = secondaryColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        val timestamp = conversation.lastMessage?.timestampMillis ?: conversation.updatedAtMillis
        if (timestamp > 0L) {
            Text(
                formatRelativeTime(timestamp, currentTimeMillis()),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Normal,
                color = secondaryColor
            )
        }
    }
}
