package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.BulletinSpacing

@Composable
internal fun MessagesInboxContent(
    state: MessagesUiState,
    inbox: InboxState,
    viewModel: MessagesViewModel,
    modifier: Modifier
) {
    Column(
        modifier.fillMaxSize()
            .padding(horizontal = BulletinSpacing.l)
            .padding(top = CONVERSATION_TITLE_SPACING)
    ) {
        if (state.connectionState == MessageConnectionState.DISCONNECTED ||
            inbox.connectionState == InboxConnectionState.DISCONNECTED
        ) {
            Text(
                "Live updates unavailable. Recent messages may be delayed.",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(vertical = BulletinSpacing.s)
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

private val CONVERSATION_TITLE_SPACING = 8.dp

@Composable
private fun CenterMessage(text: String, loading: Boolean = false, modifier: Modifier = Modifier.fillMaxSize()) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (loading) CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            Text(
                text,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(BulletinSpacing.l)
            )
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
