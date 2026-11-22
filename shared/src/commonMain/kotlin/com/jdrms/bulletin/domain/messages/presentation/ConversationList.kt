package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId

@Composable
internal fun ConversationList(state: MessagesUiState, viewModel: MessagesViewModel, modifier: Modifier) {
    val inbox by viewModel.inboxState.collectAsState()
    val listState = rememberLazyListState()
    LaunchedEffect(inbox.conversations.size, inbox.hasMore, inbox.isLoadingMore, state.unmatchedReviewRequests.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }.collect { last ->
            val loadThreshold = state.unmatchedReviewRequests.size + inbox.conversations.size - 2
            if (last >= loadThreshold && last >= 0) viewModel.loadMoreConversations()
        }
    }
    val reviews by viewModel.reviews.state.collectAsState()
    var pendingRemoval by remember { mutableStateOf<ConversationId?>(null) }
    LazyColumn(state = listState, modifier = modifier, verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)) {
        items(state.unmatchedReviewRequests, key = { "review:${it.listingId.value}:${it.sellerId.value}" }) { request ->
            UnmatchedReviewCard(request, state, viewModel.reviews)
        }
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
                listingPreviewUrl = conversation.listingThumbnailUrl
                    ?: conversation.listingId?.value?.let(state.listingPreviews::get)?.imageUrl,
                onClick = { viewModel.selectConversation(conversation.id) },
                onRemove = { pendingRemoval = conversation.id }
            )
        }
        if (inbox.isLoadingMore) item(key = "loading-more") { ConversationSkeleton() }
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
