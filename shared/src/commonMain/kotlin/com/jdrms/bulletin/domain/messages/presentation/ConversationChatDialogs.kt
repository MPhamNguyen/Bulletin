package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.runtime.Composable
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant

@Composable
internal fun ConversationChatDialogs(
    counterpart: ConversationParticipant,
    block: ConversationBlockUiState,
    options: ConversationOptionsVisibility,
    viewModel: MessagesViewModel,
    navigation: ConversationNavigation
) {
    if (options.isOpen) {
        ConversationOptionsDialog(
            ConversationOptionsActions(
                onToggleMute = null,
                onReportUser = {
                    options.onDismiss()
                    navigation.onReportUser(counterpart.id.value)
                },
                onBlockUser = {
                    options.onDismiss()
                    viewModel.requestBlock(counterpart.blockTarget())
                },
                onDismiss = options.onDismiss
            )
        )
    }
    val flow = block.flow
    val target = when (flow) {
        is ConversationBlockFlow.Confirming -> flow.target
        is ConversationBlockFlow.Submitting -> flow.target.takeIf { flow.action == BlockAction.BLOCK }
        ConversationBlockFlow.Idle -> null
    }
    if (target?.userId == counterpart.id) {
        BlockConversationUserDialog(
            target.displayName,
            flow is ConversationBlockFlow.Submitting,
            (flow as? ConversationBlockFlow.Confirming)?.errorMessage,
            viewModel.confirmBlock,
            viewModel.dismissBlock
        )
    }
}

internal data class ConversationOptionsVisibility(val isOpen: Boolean, val onDismiss: () -> Unit)
