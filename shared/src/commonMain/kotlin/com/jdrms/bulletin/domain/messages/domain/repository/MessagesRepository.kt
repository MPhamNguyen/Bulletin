package com.jdrms.bulletin.domain.messages.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageId

interface MessagesRepository {
    suspend fun getConversations(userId: String): List<Conversation>
    suspend fun getMessages(conversationId: ConversationId): List<Message>
    suspend fun sendMessage(conversationId: ConversationId, message: Message): Result<Message>
    suspend fun reportMessage(messageId: MessageId, reason: String): Result<Unit>
}
