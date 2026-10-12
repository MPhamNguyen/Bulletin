package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.formatRelativeTime
import com.jdrms.bulletin.core.designsystem.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.BulletinTextFieldDefaults
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

@Composable
internal fun MessagesContent(state: MessagesUiState, viewModel: MessagesViewModel, modifier: Modifier) {
    MessagesFeedback(state, viewModel::loadConversations)

    when {
        state.isLoading -> MessagesLoading()
        state.conversations.isEmpty() && state.errorMessage == null -> EmptyConversations()
        else -> MessagesConversationLayout(state, viewModel, modifier)
    }
}

@Composable
private fun MessagesLoading() {
    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    Text("Loading conversations...", style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun EmptyConversations() {
    BulletinCard {
        Text(
            text = "No active conversations found.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MessagesConversationLayout(state: MessagesUiState, viewModel: MessagesViewModel, modifier: Modifier) {
    Row(modifier = modifier) {
        ConversationList(state, viewModel, Modifier.weight(0.4f).padding(end = 8.dp))
        MessageThread(state, viewModel, Modifier.weight(0.6f))
    }
}

@Composable
private fun ConversationList(state: MessagesUiState, viewModel: MessagesViewModel, modifier: Modifier) {
    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(state.conversations, key = { it.id.value }) { conversation ->
            ConversationCard(
                conversation = conversation,
                viewerId = state.viewerId,
                isSelected = conversation.id == state.selectedConversationId,
                onClick = { viewModel.selectConversation(conversation.id) }
            )
        }
    }
}

@Composable
private fun ConversationCard(
    conversation: Conversation,
    viewerId: SenderId?,
    isSelected: Boolean,
    onClick: () -> Unit
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
            Text(
                text = conversationTitle(conversation, viewerId),
                fontWeight = FontWeight.Bold,
                color = selectedColor
            )
            Text(
                text = conversationPreviewText(conversation),
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            conversation.lastMessage?.let { message ->
                Text(
                    text = formatRelativeTime(message.timestampMillis, currentTimeMillis()),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MessageThread(state: MessagesUiState, viewModel: MessagesViewModel, modifier: Modifier) {
    Column(modifier = modifier) {
        val messageListState = rememberLazyListState()
        if (state.isLoadingMessages) {
            MessageThreadLoading()
        } else {
            MessageList(state, viewModel, messageListState, Modifier.weight(1f).padding(4.dp))
        }
        MessageComposer(state, viewModel)
    }
}

@Composable
private fun MessageThreadLoading() {
    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    Text("Loading messages...", style = MaterialTheme.typography.bodyMedium)
}

@Composable
private fun MessageList(
    state: MessagesUiState,
    viewModel: MessagesViewModel,
    messageListState: androidx.compose.foundation.lazy.LazyListState,
    modifier: Modifier
) {
    LaunchedEffect(
        state.selectedConversationId,
        state.currentMessages.lastOrNull()?.id,
        state.currentMessages.size
    ) {
        if (state.currentMessages.isNotEmpty()) {
            messageListState.scrollToItem(state.currentMessages.lastIndex)
        }
    }
    LazyColumn(
        modifier = modifier,
        state = messageListState,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(state.currentMessages, key = { it.id.value }) { message ->
            MessageItemCard(
                message = message,
                isRevealed = message.id in state.revealedReportedMessageIds,
                canReport = !state.isReporting,
                onReport = { viewModel.report(message.id, "Inappropriate content") },
                onToggleVisibility = { viewModel.toggleReportedMessageVisibility(message.id) }
            )
        }
    }
}

@Composable
private fun MessageComposer(state: MessagesUiState, viewModel: MessagesViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = state.messageInput,
            onValueChange = viewModel::onMessageInputChanged,
            placeholder = { Text("Type a message...") },
            singleLine = true,
            enabled = !state.isSending,
            colors = BulletinTextFieldDefaults.colors(),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Button(
            onClick = viewModel::sendCurrentMessage,
            enabled = !state.isSending && !state.isLoadingMessages && state.messageInput.isNotBlank(),
            colors = BulletinButtonDefaults.buttonColors()
        ) {
            Text(if (state.isSending) "Sending..." else "Send")
        }
    }
}

@Composable
private fun MessagesFeedback(state: MessagesUiState, onRetry: () -> Unit) {
    state.errorMessage?.let { error ->
        BulletinCard {
            Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = onRetry) { Text("Retry") }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
    state.statusMessage?.let { status ->
        Text(status, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
private fun MessageItemCard(
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
            MessageMetadataText(
                text = message.senderName,
                isReported = message.isReported,
                fontWeight = FontWeight.Bold
            )
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
                text = formatRelativeTime(message.timestampMillis, currentTimeMillis()),
                isReported = message.isReported,
                fontWeight = null
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
