package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.domain.model.SenderId

data class BlockTarget(val userId: SenderId, val displayName: String)
enum class BlockAction { BLOCK, UNBLOCK }

sealed interface ConversationBlockFlow {
    data object Idle : ConversationBlockFlow
    data class Confirming(val target: BlockTarget, val errorMessage: String? = null) : ConversationBlockFlow
    data class Submitting(val target: BlockTarget, val action: BlockAction) : ConversationBlockFlow
}

data class ConversationBlockUiState(
    val statusRequired: Boolean = false,
    val blockedUserIds: Set<SenderId> = emptySet(),
    val checkedUserIds: Set<SenderId> = emptySet(),
    val flow: ConversationBlockFlow = ConversationBlockFlow.Idle,
    val statusErrorMessage: String? = null,
    val unblockErrorMessage: String? = null
) {
    fun isBlocked(userId: SenderId): Boolean = userId in blockedUserIds
}
