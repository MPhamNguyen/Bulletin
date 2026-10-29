package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.BulletinSpacing
import com.jdrms.bulletin.domain.messages.domain.model.Conversation

@Composable
internal fun MessagesChatContent(
    state: MessagesUiState,
    viewModel: MessagesViewModel,
    conversation: Conversation,
    modifier: Modifier,
    navigation: ConversationNavigation = ConversationNavigation()
) {
    val counterpart = conversation.counterpart(state.viewerId)
    val isBlocked = counterpart != null && state.block.isBlocked(counterpart.id)
    var optionsOpen by remember(conversation.id, state.viewerId) { mutableStateOf(false) }
    LaunchedEffect(counterpart?.id, state.viewerId) { counterpart?.let { viewModel.refreshBlockStatus(it.id) } }
    Column(modifier.imePadding()) {
        ConversationHeader(
            counterpart,
            conversationTitle(conversation, state.viewerId),
            counterpart != null && !isBlocked,
            ConversationHeaderActions(
                viewModel::returnToInbox,
                { navigation.onViewProfile(it.id.value) },
                { optionsOpen = true }
            )
        )
        conversation.listingId?.let { listingId ->
            ConversationListingCard(
                state.listingPreviews[listingId.value],
                modifier = Modifier.padding(horizontal = BulletinSpacing.gutter, vertical = BulletinSpacing.s)
            )
        }
        ChatStatusMessages(state, viewModel)
        MessageList(
            state,
            viewModel,
            conversation,
            rememberMessageHistoryListState(viewModel, state),
            Modifier.weight(1f).padding(horizontal = BulletinSpacing.gutter),
            navigation.onViewProfile
        )
        if (counterpart != null && isBlocked) {
            BlockedConversationBar(
                counterpart.displayName,
                state.block.flow is ConversationBlockFlow.Submitting,
                state.block.unblockErrorMessage,
                { viewModel.unblock(counterpart.blockTarget()) }
            )
        } else {
            MessageComposer(state, viewModel)
        }
    }
    if (counterpart != null) {
        ConversationChatDialogs(
            counterpart,
            state.block,
            ConversationOptionsVisibility(optionsOpen) { optionsOpen = false },
            viewModel,
            navigation
        )
    }
}

@Composable
private fun ChatStatusMessages(state: MessagesUiState, viewModel: MessagesViewModel) {
    val error = state.errorMessage ?: state.block.statusErrorMessage
    error?.let {
        Row(Modifier.padding(horizontal = BulletinSpacing.gutter), verticalAlignment = Alignment.CenterVertically) {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
            TextButton(onClick = {
                if (state.block.statusErrorMessage != null) {
                    state.conversations.firstOrNull { it.id == state.selectedConversationId }
                        ?.counterpart(state.viewerId)?.let { viewModel.refreshBlockStatus(it.id) }
                } else {
                    viewModel.loadConversations()
                }
            }) { Text("Retry") }
        }
    }
    state.statusMessage?.let {
        Text(
            it,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = BulletinSpacing.gutter)
        )
    }
}
