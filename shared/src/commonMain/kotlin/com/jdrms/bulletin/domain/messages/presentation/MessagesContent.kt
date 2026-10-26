package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.BulletinTextFieldDefaults
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

@Composable
internal fun MessagesInboxContent(
    state: MessagesUiState,
    inbox: InboxState,
    viewModel: MessagesViewModel,
    modifier: Modifier
) {
    val refreshState = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = inbox.isLoading,
        onRefresh = viewModel::loadConversations,
        state = refreshState,
        modifier = modifier
    ) {
        Column(
            Modifier.fillMaxSize()
                .offset(y = (refreshState.distanceFraction.coerceIn(0f, 1f) * CONVERSATION_CONTENT_PULL_OFFSET).dp)
                .padding(horizontal = 16.dp)
                .padding(top = CONVERSATION_TITLE_SPACING)
        ) {
            if (state.connectionState == MessageConnectionState.DISCONNECTED ||
                inbox.connectionState == InboxConnectionState.DISCONNECTED
            ) {
                Text(
                    "Live updates unavailable. Recent messages may be delayed.",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            state.errorMessage?.let { error ->
                BulletinCard {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    TextButton(onClick = viewModel::loadConversations) { Text("Retry") }
                }
            }
            when {
                state.isLoading && state.conversations.isEmpty() -> CenterMessage(
                    "Loading conversations...",
                    loading = true
                )
                state.conversations.isEmpty() -> CenterMessage(
                    "No conversations yet. Message a seller to start a conversation."
                )
                else -> ConversationList(state, viewModel, Modifier.weight(1f).fillMaxWidth())
            }
        }
    }
}

private const val CONVERSATION_CONTENT_PULL_OFFSET = 64
private val CONVERSATION_TITLE_SPACING = 8.dp

@Composable
internal fun MessagesChatContent(
    state: MessagesUiState,
    viewModel: MessagesViewModel,
    conversation: Conversation,
    modifier: Modifier,
    onViewProfile: (String) -> Unit = {}
) {
    Column(modifier = modifier.imePadding()) {
        ConversationParticipantsSection(conversation, state.viewerId, onViewProfile)
        conversation.listingId?.let { listingId ->
            ConversationListingCard(
                preview = state.listingPreviews[listingId.value],
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        state.errorMessage?.let { error ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(error, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                TextButton(onClick = viewModel::loadConversations) { Text("Retry") }
            }
        }
        if (state.statusMessage != null) {
            Text(
                state.statusMessage,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
        val listState = rememberMessageHistoryListState(viewModel, state)
        MessageList(state, viewModel, conversation, listState, Modifier.weight(1f).padding(horizontal = 12.dp))
        MessageComposer(state, viewModel)
    }
}

@Composable
private fun ConversationParticipantsSection(
    conversation: Conversation,
    viewerId: SenderId?,
    onViewProfile: (String) -> Unit
) {
    val currentViewerId = viewerId ?: return
    val participants = conversation.participants.filter { it.id != currentViewerId }
    participants.forEach { participant ->
        Surface(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clickable { onViewProfile(participant.id.value) },
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ConversationAvatar(participant.displayName)
                Column {
                    Text(participant.displayName, style = MaterialTheme.typography.titleSmall)
                    Text(
                        "View profile",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun CenterMessage(text: String, loading: Boolean = false, modifier: Modifier = Modifier.fillMaxSize()) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (loading) CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
        }
    }
}

@Composable
internal fun rememberMessageHistoryListState(
    viewModel: MessagesViewModel,
    state: MessagesUiState
): androidx.compose.foundation.lazy.LazyListState {
    val listState = rememberLazyListState()
    var positioned by remember(state.selectedConversationId) { mutableStateOf(false) }
    LaunchedEffect(state.selectedConversationId, state.currentMessages.lastOrNull()?.id) {
        val targetIndex = messageScrollTargetIndex(state.currentMessages.size, state.olderCursor != null)
        if (targetIndex != null) {
            val layoutInfo = listState.layoutInfo
            val lastVisibleItemIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            if (shouldScrollToLatest(
                    isInitialPosition = !positioned,
                    lastVisibleItemIndex = lastVisibleItemIndex,
                    totalItemsCount = layoutInfo.totalItemsCount
                )
            ) {
                listState.scrollToItem(targetIndex)
            }
            positioned = true
        }
    }
    LaunchedEffect(state.selectedConversationId, state.olderCursor, positioned) {
        if (positioned && state.olderCursor != null) {
            var previousFirstVisible: Int? = null
            snapshotFlow { listState.firstVisibleItemIndex }.collect { firstVisible ->
                if (shouldLoadOlderAfterScroll(previousFirstVisible, firstVisible)) viewModel.loadOlderMessages()
                previousFirstVisible = firstVisible
            }
        }
    }
    return listState
}

@Composable
private fun MessageComposer(state: MessagesUiState, viewModel: MessagesViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = state.messageInput,
            onValueChange = viewModel::onMessageInputChanged,
            placeholder = { Text("Type a message...") },
            enabled = !state.isSending,
            maxLines = 4,
            colors = BulletinTextFieldDefaults.colors(),
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Button(
            onClick = viewModel::sendCurrentMessage,
            enabled = !state.isSending && !state.isLoadingMessages && state.messageInput.isNotBlank(),
            colors = BulletinButtonDefaults.buttonColors()
        ) { Text(if (state.isSending) "Sending..." else "Send") }
    }
}
