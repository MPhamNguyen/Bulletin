package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId

@Composable
internal fun ConversationList(state: MessagesUiState, viewModel: MessagesViewModel, modifier: Modifier) {
    val reviews by viewModel.reviews.state.collectAsState()
    var pendingRemoval by remember { mutableStateOf<ConversationId?>(null) }
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)) {
        items(
            state.conversations.sortedByDescending { conversation ->
                conversationInboxAttention(conversation, state, reviews).hasPendingReview
            },
            key = { it.id.value }
        ) { conversation ->
            ConversationCard(
                conversation = conversation,
                viewerId = state.viewerId,
                isSelected = state.isConversationOpen && conversation.id == state.selectedConversationId,
                attention = conversationInboxAttention(conversation, state, reviews),
                listingPreviewUrl = conversation.listingId?.value?.let(state.listingPreviews::get)?.imageUrl,
                onClick = { viewModel.selectConversation(conversation.id) },
                onRemove = { pendingRemoval = conversation.id }
            )
        }
    }
    pendingRemoval?.let { conversationId ->
        RemoveConversationDialog(
            onDismiss = { pendingRemoval = null },
            onConfirm = {
                pendingRemoval = null
                viewModel.hideConversationFromInbox(conversationId)
            }
        )
    }
}
