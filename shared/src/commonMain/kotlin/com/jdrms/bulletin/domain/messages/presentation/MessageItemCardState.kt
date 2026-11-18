package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.ui.unit.Dp

internal data class MessageItemCardPresentation(
    val maxBubbleWidth: Dp,
    val isMine: Boolean,
    val showTimestamp: Boolean,
    val showSeenReceipt: Boolean,
    val showAvatar: Boolean,
    val isRevealed: Boolean,
    val canReport: Boolean,
    val conversationTitle: String
)

internal data class MessageItemCardActions(
    val onReport: () -> Unit,
    val onToggleVisibility: () -> Unit,
    val onEdit: () -> Unit = {},
    val onViewSenderProfile: () -> Unit = {},
    val editingDisabled: Boolean = false
)
