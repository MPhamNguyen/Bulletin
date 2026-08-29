package com.jdrms.bulletin.domain.messages.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.domain.service.MessagesPolicy

class GetConversations(
    private val repository: MessagesRepository
) {
    suspend operator fun invoke(userId: String = "current_student"): List<Conversation> {
        return repository.getConversations(userId)
    }
}

class GetConversationMessages(
    private val repository: MessagesRepository
) {
    suspend operator fun invoke(conversationId: ConversationId): List<Message> {
        return repository.getMessages(conversationId)
    }
}

class SendMessage(
    private val repository: MessagesRepository,
    private val policy: MessagesPolicy = MessagesPolicy()
) {
    suspend operator fun invoke(conversationId: ConversationId, message: Message): Result<Message> {
        val validation = policy.validateMessageContent(message.content)
        if (validation.isError()) {
            return Result.Error((validation as Result.Error).exception)
        }
        return repository.sendMessage(conversationId, message)
    }
}

class ReportMessage(
    private val repository: MessagesRepository,
    private val policy: MessagesPolicy = MessagesPolicy()
) {
    suspend operator fun invoke(messageId: MessageId, reason: String): Result<Unit> {
        val validation = policy.validateReportReason(reason)
        if (validation.isError()) {
            return Result.Error((validation as Result.Error).exception)
        }
        return repository.reportMessage(messageId, reason)
    }
}
