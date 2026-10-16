package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository

internal suspend fun MessagesRepository.getMessages(
    userId: SenderId,
    conversationId: ConversationId
): Result<List<Message>> = when (val page = getMessagePage(userId, conversationId)) {
    is Result.Success -> Result.Success(page.data.messages)
    is Result.Error -> page
}
