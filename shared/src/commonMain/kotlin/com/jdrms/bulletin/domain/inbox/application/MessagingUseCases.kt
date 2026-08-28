package com.jdrms.bulletin.domain.inbox.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.inbox.domain.model.*
import com.jdrms.bulletin.domain.inbox.domain.repository.MessagingRepository
import com.jdrms.bulletin.domain.inbox.domain.service.MessagingPolicy

class GetConversations(
    private val messagingRepository: MessagingRepository
) {
    suspend operator fun invoke(userId: ParticipantId): List<Conversation> {
        return messagingRepository.getConversations(userId)
    }

    suspend fun getMessages(conversationId: ConversationId): List<Message> {
        return messagingRepository.getMessages(conversationId)
    }
}

class SendMessage(
    private val messagingRepository: MessagingRepository,
    private val messagingPolicy: MessagingPolicy = MessagingPolicy()
) {
    suspend operator fun invoke(
        conversationId: ConversationId,
        senderId: ParticipantId,
        senderName: String,
        text: String
    ): Result<Message> {
        val validation = messagingPolicy.validateMessageContent(text)
        if (validation.isError()) {
            return Result.Error((validation as Result.Error).exception)
        }
        return messagingRepository.sendMessage(conversationId, senderId, senderName, text)
    }
}

class ReportMessage(
    private val messagingRepository: MessagingRepository,
    private val messagingPolicy: MessagingPolicy = MessagingPolicy()
) {
    suspend operator fun invoke(messageId: MessageId, reason: String): Result<Unit> {
        val validation = messagingPolicy.validateReportReason(reason)
        if (validation.isError()) {
            return Result.Error((validation as Result.Error).exception)
        }
        return messagingRepository.reportMessage(messageId, reason)
    }
}
