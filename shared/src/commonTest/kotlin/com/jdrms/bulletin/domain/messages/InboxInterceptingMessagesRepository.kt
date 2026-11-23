package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.map
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository

/** Routes inbox reads through the overridden snapshot used by failure and visibility fixtures. */
internal open class InboxInterceptingMessagesRepository(
    private val backing: MessagesRepository
) : MessagesRepository by backing {
    override suspend fun getConversations(
        userId: SenderId
    ): Result<List<Conversation>> = backing.getConversations(userId)

    override suspend fun getConversationPage(userId: SenderId, offset: Int, limit: Int) =
        getConversations(userId).map { rows -> rows.sortedByDescending { it.updatedAtMillis }.drop(offset).take(limit) }

    override suspend fun getConversation(userId: SenderId, conversationId: ConversationId) =
        getConversations(userId).map { rows -> rows.firstOrNull { it.id == conversationId } }
}
