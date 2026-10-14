package com.jdrms.bulletin.domain.messages.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository

/** Removes a conversation from only the current participant's inbox. */
class HideConversation(
    private val repository: MessagesRepository,
    private val currentSenderProvider: CurrentMessageSenderProvider
) {
    suspend operator fun invoke(conversationId: ConversationId): Result<Unit> =
        when (val sender = currentSenderProvider.getCurrentSender()) {
            is Result.Success -> repository.hideConversation(sender.data.id, conversationId)
            is Result.Error -> sender
        }
}
