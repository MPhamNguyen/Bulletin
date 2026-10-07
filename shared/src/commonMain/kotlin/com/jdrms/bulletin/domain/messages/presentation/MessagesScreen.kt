package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.BulletinTextFieldDefaults
import com.jdrms.bulletin.core.designsystem.SectionHeader
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

@Composable
fun MessagesScreen(viewModel: MessagesViewModel) {
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SectionHeader(
            title = "Messages",
            subtitle = "Conversations with campus buyers and sellers"
        )

        Spacer(modifier = Modifier.height(8.dp))

        MessagesFeedback(state, viewModel::loadConversations)

        if (state.isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text("Loading conversations...", style = MaterialTheme.typography.bodyMedium)
        } else if (state.conversations.isEmpty() && state.errorMessage == null) {
            BulletinCard {
                Text(
                    text = "No active conversations found.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Row(modifier = Modifier.weight(1f)) {
                // Conversation list column
                LazyColumn(
                    modifier = Modifier.weight(0.4f).padding(end = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(state.conversations, key = { it.id.value }) { conv ->
                        val isSelected = conv.id == state.selectedConversationId
                        Card(
                            modifier = Modifier.fillMaxWidth().clickable { viewModel.selectConversation(conv.id) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                contentColor = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = conversationTitle(conv, state.viewerId),
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                                Text(
                                    text = conv.lastMessage?.content ?: "No messages yet",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isSelected) {
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                // Messages view column
                Column(modifier = Modifier.weight(0.6f)) {
                    val messageListState = rememberLazyListState()
                    if (state.isLoadingMessages) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        Text("Loading messages...", style = MaterialTheme.typography.bodyMedium)
                    } else {
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
                            modifier = Modifier.weight(1f).padding(4.dp),
                            state = messageListState,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(state.currentMessages, key = { it.id.value }) { msg ->
                                MessageItemCard(
                                    message = msg,
                                    canReport = !state.isReporting,
                                    onReport = { viewModel.report(msg.id, "Inappropriate content") }
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = state.messageInput,
                            onValueChange = { viewModel.onMessageInputChanged(it) },
                            placeholder = { Text("Type a message...") },
                            singleLine = true,
                            enabled = !state.isSending,
                            colors = BulletinTextFieldDefaults.colors(),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = { viewModel.sendCurrentMessage() },
                            enabled = !state.isSending && !state.isLoadingMessages && state.messageInput.isNotBlank(),
                            colors = BulletinButtonDefaults.buttonColors()
                        ) {
                            Text(if (state.isSending) "Sending..." else "Send")
                        }
                    }
                }
            }
        }
    }
}

internal fun conversationTitle(conversation: Conversation, viewerId: SenderId?): String {
    val otherNames = viewerId?.let(conversation::otherParticipantNames).orEmpty()
    return otherNames.joinToString(", ").ifBlank { "Conversation" }
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
    canReport: Boolean,
    onReport: () -> Unit
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
            Text(
                text = message.senderName,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (message.isReported) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Text(
                text = message.content,
                style = MaterialTheme.typography.bodyMedium,
                color = if (message.isReported) {
                    MaterialTheme.colorScheme.onErrorContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )
            if (message.isReported) {
                Text(
                    text = "[Reported for moderation]",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                TextButton(
                    onClick = onReport,
                    enabled = canReport,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Report", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
