package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.SectionHeader
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

@Composable
fun MessagesScreen(viewModel: MessagesViewModel) {
    DisposableEffect(viewModel) {
        viewModel.onDestinationEntered()
        onDispose { viewModel.onDestinationLeft() }
    }
    val state by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        SectionHeader(
            title = "Messages",
            subtitle = "Conversations with campus buyers and sellers"
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (state.connectionState == MessageConnectionState.DISCONNECTED) {
            Text(
                text = "Live updates unavailable. Recent messages may be delayed.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        MessagesContent(state, viewModel, Modifier.weight(1f))
    }
}

internal fun conversationTitle(conversation: Conversation, viewerId: SenderId?): String {
    val otherNames = viewerId?.let(conversation::otherParticipantNames).orEmpty()
    return otherNames.joinToString(", ").ifBlank { "Conversation" }
}

internal fun conversationPreviewText(conversation: Conversation): String =
    conversation.lastMessage?.let { messageBodyText(it, isRevealed = false) } ?: "No messages yet"

internal fun messageBodyText(message: Message, isRevealed: Boolean): String =
    if (message.isReported && !isRevealed) "Reported message hidden" else message.content
