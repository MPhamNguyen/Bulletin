package com.jdrms.bulletin.domain.inbox.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.inbox.domain.model.Conversation
import com.jdrms.bulletin.domain.inbox.domain.model.ConversationId
import com.jdrms.bulletin.domain.inbox.domain.model.Message
import com.jdrms.bulletin.domain.inbox.domain.model.MessageId
import com.jdrms.bulletin.domain.inbox.domain.model.ParticipantId

interface MessagingRepository {
    suspend fun getConversations(userId: ParticipantId): List<Conversation>
    suspend fun getMessages(conversationId: ConversationId): List<Message>
    suspend fun sendMessage(
        conversationId: ConversationId,
        senderId: ParticipantId,
        senderName: String,
        text: String
    ): Result<Message>
    suspend fun reportMessage(messageId: MessageId, reason: String): Result<Unit>
    suspend fun startConversation(
        participant1: ParticipantId,
        name1: String,
        participant2: ParticipantId,
        name2: String
    ): Result<Conversation>
}
