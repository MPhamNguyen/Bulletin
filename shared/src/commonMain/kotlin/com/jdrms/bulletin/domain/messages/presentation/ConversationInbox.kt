package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.DraggableState
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinConversationDefaults
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId

@Composable
internal fun ConversationList(state: MessagesUiState, viewModel: MessagesViewModel, modifier: Modifier) {
    var pendingRemoval by remember { mutableStateOf<ConversationId?>(null) }
    LazyColumn(modifier = modifier, verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)) {
        items(state.conversations, key = { it.id.value }) { conversation ->
            ConversationCard(
                conversation = conversation,
                viewerId = state.viewerId,
                isSelected = conversation.id == state.selectedConversationId,
                hasUnreadMessage = conversation.hasUnreadMessage(state.viewerId),
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

private fun Conversation.hasUnreadMessage(viewerId: SenderId?): Boolean {
    val latestMessage = lastMessage ?: return false
    if (viewerId == null || latestMessage.senderId == viewerId) return false
    val viewerReadAt = participants.firstOrNull { it.id == viewerId }?.lastReadAt
    return viewerReadAt == null || latestMessage.orderingTimestamp > viewerReadAt
}

@Composable
private fun ConversationCard(
    conversation: Conversation,
    viewerId: SenderId?,
    isSelected: Boolean,
    hasUnreadMessage: Boolean,
    listingPreviewUrl: String?,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    val actionWidth = 80.dp
    val actionWidthPx = with(LocalDensity.current) { actionWidth.toPx() }
    var swipeOffset by remember(conversation.id) { mutableStateOf(0f) }
    val dragState = rememberDraggableState { delta ->
        swipeOffset = (swipeOffset + delta).coerceIn(-actionWidthPx, 0f)
    }
    val selectedColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        if (swipeOffset < 0f) ConversationDeleteAction(actionWidth, onRemove)
        ConversationCardSurface(
            conversation = conversation,
            viewerId = viewerId,
            isSelected = isSelected,
            hasUnreadMessage = hasUnreadMessage,
            listingPreviewUrl = listingPreviewUrl,
            selectedColor = selectedColor,
            modifier = conversationCardModifier(
                swipeOffset = swipeOffset,
                dragState = dragState,
                onClose = { swipeOffset = 0f },
                onClick = onClick,
                onDragStopped = {
                    swipeOffset = if (swipeOffset <= -actionWidthPx * 0.4f) -actionWidthPx else 0f
                }
            )
        )
    }
}

@Composable
private fun ConversationCardSurface(
    conversation: Conversation,
    viewerId: SenderId?,
    isSelected: Boolean,
    hasUnreadMessage: Boolean,
    listingPreviewUrl: String?,
    selectedColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier
) {
    val cardShape = MaterialTheme.shapes.medium
    val cardModifier = if (hasUnreadMessage && !isSelected) {
        modifier.background(
            brush = Brush.horizontalGradient(
                colors = listOf(
                    BulletinConversationDefaults.unreadGradientStart,
                    BulletinConversationDefaults.unreadGradientEnd
                )
            ),
            shape = cardShape
        )
    } else {
        modifier
    }
    Card(
        modifier = cardModifier,
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = when {
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                hasUnreadMessage -> androidx.compose.ui.graphics.Color.Transparent
                else -> MaterialTheme.colorScheme.surface
            },
            contentColor = selectedColor
        )
    ) {
        ConversationCardContent(conversation, listingPreviewUrl, viewerId, isSelected, hasUnreadMessage, selectedColor)
    }
}

private fun conversationCardModifier(
    swipeOffset: Float,
    dragState: DraggableState,
    onClose: () -> Unit,
    onClick: () -> Unit,
    onDragStopped: () -> Unit
): Modifier = Modifier.fillMaxWidth()
    .heightIn(min = 96.dp)
    .offset { IntOffset(swipeOffset.toInt(), 0) }
    .draggable(
        state = dragState,
        orientation = Orientation.Horizontal,
        onDragStopped = { onDragStopped() }
    )
    .clickable { if (swipeOffset < 0f) onClose() else onClick() }

@Composable
private fun BoxScope.ConversationDeleteAction(actionWidth: androidx.compose.ui.unit.Dp, onRemove: () -> Unit) {
    Box(
        modifier = Modifier.align(Alignment.CenterEnd)
            .width(actionWidth)
            .heightIn(min = 96.dp)
            .background(MaterialTheme.colorScheme.errorContainer),
        contentAlignment = Alignment.Center
    ) {
        IconButton(onClick = onRemove) {
            Icon(
                Icons.Outlined.Delete,
                contentDescription = "Remove conversation",
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun ConversationCardContent(
    conversation: Conversation,
    listingPreviewUrl: String?,
    viewerId: SenderId?,
    isSelected: Boolean,
    hasUnreadMessage: Boolean,
    selectedColor: androidx.compose.ui.graphics.Color
) {
    val secondaryColor = if (isSelected) {
        selectedColor.copy(alpha = 0.7f)
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    val titleColor = if (isSelected) selectedColor else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = BulletinSpacing.l, vertical = BulletinSpacing.l),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
    ) {
        ConversationAvatar(conversationTitle(conversation, viewerId))
        ConversationSummary(conversation, viewerId, titleColor, secondaryColor, Modifier.weight(1f))
        if (hasUnreadMessage) {
            Box(Modifier.padding(end = BulletinSpacing.s)) {
                ConversationUnreadIndicator()
            }
        }
        ConversationListingPreview(listingPreviewUrl)
    }
}
