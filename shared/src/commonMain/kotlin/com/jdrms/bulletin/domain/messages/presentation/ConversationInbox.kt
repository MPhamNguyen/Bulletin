package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.formatRelativeTime
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

@Composable
internal fun ConversationList(state: MessagesUiState, viewModel: MessagesViewModel, modifier: Modifier) {
    var pendingRemoval by remember { mutableStateOf<ConversationId?>(null) }
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items(state.conversations, key = { it.id.value }) { conversation ->
            ConversationCard(
                conversation = conversation,
                viewerId = state.viewerId,
                isSelected = conversation.id == state.selectedConversationId,
                onClick = { viewModel.selectConversation(conversation.id) },
                onRemove = { pendingRemoval = conversation.id }
            )
        }
    }
    pendingRemoval?.let { conversationId ->
        AlertDialog(
            onDismissRequest = { pendingRemoval = null },
            title = { Text("Remove conversation?") },
            text = { Text("This removes it from your inbox only. The other person keeps their conversation.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingRemoval = null
                    viewModel.hideConversationFromInbox(conversationId)
                }) {
                    Text("Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoval = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ConversationCard(
    conversation: Conversation,
    viewerId: SenderId?,
    isSelected: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val selectedColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            },
            contentColor = selectedColor
        )
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(conversationTitle(conversation, viewerId), fontWeight = FontWeight.Bold, color = selectedColor)
            Text(
                conversationPreviewText(conversation),
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            conversation.lastMessage?.let { message ->
                Text(
                    formatRelativeTime(message.timestampMillis, currentTimeMillis()),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onRemove) {
                Text("Remove", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}
