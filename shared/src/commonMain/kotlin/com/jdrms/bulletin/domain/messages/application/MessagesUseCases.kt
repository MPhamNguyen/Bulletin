package com.jdrms.bulletin.domain.messages.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.generateUuid
import com.jdrms.bulletin.core.common.map
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.domain.service.MessagesPolicy
import kotlinx.coroutines.sync.withLock

class GetConversations(
    private val repository: MessagesRepository,
    private val currentSenderProvider: CurrentMessageSenderProvider
) {
    suspend operator fun invoke(): Result<List<Conversation>> {
        return when (val sender = currentSenderProvider.getCurrentSender()) {
            is Result.Success -> repository.getConversations(sender.data.id)
            is Result.Error -> sender
        }
    }
}

class GetConversationMessages(
    private val repository: MessagesRepository,
    private val currentSenderProvider: CurrentMessageSenderProvider
) {
    suspend operator fun invoke(conversationId: ConversationId): Result<List<Message>> {
        return when (val sender = currentSenderProvider.getCurrentSender()) {
            is Result.Success -> repository.getMessages(sender.data.id, conversationId)
            is Result.Error -> sender
        }
    }
}

class HasListingConversation(
    private val repository: MessagesRepository,
    private val currentSenderProvider: CurrentMessageSenderProvider
) {
    suspend operator fun invoke(
        listingId: ListingReferenceId,
        sellerId: SenderId
    ): Result<Boolean> {
        val sender = when (val result = currentSenderProvider.getCurrentSender()) {
            is Result.Success -> result.data
            is Result.Error -> return result
        }
        return repository.getConversations(sender.id).map { conversations ->
            conversations.any { conversation ->
                conversation.listingId == listingId && conversation.includes(sellerId)
            }
        }
    }
}

class SendMessage(
    private val repository: MessagesRepository,
    private val currentSenderProvider: CurrentMessageSenderProvider,
    private val policy: MessagesPolicy = MessagesPolicy(),
    private val nextMessageId: () -> MessageId = { MessageId(generateUuid()) },
    private val nowMillis: () -> Long = ::currentTimeMillis
) {
    suspend operator fun invoke(conversationId: ConversationId, content: String): Result<Message> {
        val sender = when (val result = currentSenderProvider.getCurrentSender()) {
            is Result.Success -> result.data
            is Result.Error -> return result
        }
        val trimmedContent = content.trim()
        val validation = policy.validateMessageContent(trimmedContent)
        if (validation is Result.Error) return validation
        return repository.sendMessage(
            Message(
                id = nextMessageId(),
                conversationId = conversationId,
                senderId = sender.id,
                senderName = sender.displayName,
                content = trimmedContent,
                timestampMillis = nowMillis()
            )
        )
    }
}

class MessageSeller(
    private val repository: MessagesRepository,
    private val currentSenderProvider: CurrentMessageSenderProvider,
    private val policy: MessagesPolicy = MessagesPolicy(),
    private val nextMessageId: () -> MessageId = { MessageId(generateUuid()) },
    private val nowMillis: () -> Long = ::currentTimeMillis
) {
    private val sendLock = kotlinx.coroutines.sync.Mutex()

    suspend operator fun invoke(
        listingId: ListingReferenceId,
        seller: ConversationParticipant,
        content: String
    ): Result<Conversation> = sendLock.withLock {
        val sender = when (val result = currentSenderProvider.getCurrentSender()) {
            is Result.Success -> result.data
            is Result.Error -> return@withLock result
        }
        if (sender.id == seller.id) {
            return@withLock Result.Error(
                IllegalArgumentException("You cannot message yourself.")
            )
        }
        val trimmedContent = content.trim()
        val validation = policy.validateMessageContent(trimmedContent)
        if (validation is Result.Error) {
            return@withLock validation
        }
        when (val conversation = repository.getOrCreateConversation(sender.id, seller, listingId)) {
            is Result.Error -> conversation
            is Result.Success -> when (
                val sent = repository.sendMessage(
                    Message(
                        id = nextMessageId(),
                        conversationId = conversation.data.id,
                        senderId = sender.id,
                        senderName = sender.displayName,
                        content = trimmedContent,
                        timestampMillis = nowMillis()
                    )
                )
            ) {
                is Result.Error -> sent
                is Result.Success -> conversation
            }
        }
    }
}

class ReportMessage(
    private val repository: MessagesRepository,
    private val currentSenderProvider: CurrentMessageSenderProvider,
    private val policy: MessagesPolicy = MessagesPolicy()
) {
    suspend operator fun invoke(conversationId: ConversationId, messageId: MessageId, reason: String): Result<Unit> {
        val sender = when (val result = currentSenderProvider.getCurrentSender()) {
            is Result.Success -> result.data
            is Result.Error -> return result
        }
        val validation = policy.validateReportReason(reason)
        if (validation is Result.Error) return validation
        return repository.reportMessage(sender.id, conversationId, messageId, reason)
    }
}
