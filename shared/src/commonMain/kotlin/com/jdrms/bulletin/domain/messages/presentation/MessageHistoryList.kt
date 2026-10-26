package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.common.conversationLocalDayKey
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.formatConversationDate
import com.jdrms.bulletin.domain.messages.domain.model.Conversation

@Composable
internal fun MessageList(
    state: MessagesUiState,
    viewModel: MessagesViewModel,
    conversation: Conversation,
    listState: LazyListState,
    modifier: Modifier
) {
    val messages = state.currentMessages
    val latestOutgoingMessageId = messages.lastOrNull { it.senderId == state.viewerId }?.id
    val otherParticipantReadAt = conversation.participants
        .filterNot { it.id == state.viewerId }
        .mapNotNull { it.lastReadAt }
        .maxOrNull()
    BoxWithConstraints(modifier = modifier) {
        val maxBubbleWidth = maxWidth * 0.75f
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.Bottom)
        ) {
            if (state.olderCursor != null) {
                item(key = "older-messages") {
                    TextButton(
                        onClick = viewModel::loadOlderMessages,
                        enabled = !state.isLoadingOlder,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (state.isLoadingOlder) "Loading..." else "Load older messages")
                    }
                }
            }
            itemsIndexed(messages, key = { _, message -> message.id.value }) { index, message ->
                val previous = messages.getOrNull(index - 1)
                val next = messages.getOrNull(index + 1)
                val isMine = message.senderId == state.viewerId
                val showSeenReceipt = isMine && message.id == latestOutgoingMessageId &&
                    otherParticipantReadAt != null && message.orderingTimestamp <= otherParticipantReadAt
                val isLastInGroup = next == null || next.senderId != message.senderId ||
                    conversationLocalDayKey(next.timestampMillis) != conversationLocalDayKey(message.timestampMillis)
                Column {
                    if (previous == null || conversationLocalDayKey(previous.timestampMillis) !=
                        conversationLocalDayKey(message.timestampMillis)
                    ) {
                        DateSeparator(message.timestampMillis)
                    }
                    MessageItemCard(
                        message = message,
                        presentation = MessageItemCardPresentation(
                            maxBubbleWidth = maxBubbleWidth,
                            isMine = isMine,
                            showTimestamp = isLastInGroup,
                            showSeenReceipt = showSeenReceipt,
                            showAvatar = !isMine && isLastInGroup,
                            isRevealed = message.id in state.revealedReportedMessageIds,
                            canReport = !state.isReporting,
                            conversationTitle = conversationTitle(conversation, state.viewerId)
                        ),
                        actions = MessageItemCardActions(
                            onReport = { viewModel.report(message.id) },
                            onToggleVisibility = { viewModel.toggleReportedMessageVisibility(message.id) }
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DateSeparator(timestampMillis: Long) {
    Text(
        formatConversationDate(timestampMillis, currentTimeMillis()),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        textAlign = TextAlign.Center
    )
}
