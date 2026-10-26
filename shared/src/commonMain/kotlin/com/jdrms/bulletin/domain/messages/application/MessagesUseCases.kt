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
import com.jdrms.bulletin.domain.messages.domain.model.MessageCursor
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.MessagePage
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.domain.service.MessagesPolicy
import kotlinx.coroutines.sync.withLock
import kotlin.time.Instant

data class MessageInbox(val viewerId: SenderId, val conversations: List<Conversation>)

class GetConversations(
    private val repository: MessagesRepository,
    private val currentSenderProvider: CurrentMessageSenderProvider
) {
    suspend operator fun invoke(): Result<MessageInbox> {
        return when (val sender = currentSenderProvider.getCurrentSender()) {
            is Result.Success -> when (val conversations = repository.getConversations(sender.data.id)) {
                is Result.Success -> Result.Success(MessageInbox(sender.data.id, conversations.data))
                is Result.Error -> conversations
            }
            is Result.Error -> sender
        }
    }
}

class GetConversationMessages(
    private val repository: MessagesRepository,
    private val currentSenderProvider: CurrentMessageSenderProvider
) {
    suspend operator fun invoke(
        conversationId: ConversationId,
        before: MessageCursor? = null,
        through: MessageCursor? = null
    ): Result<MessagePage> {
        return when (val sender = currentSenderProvider.getCurrentSender()) {
            is Result.Success -> loadPages(sender.data.id, conversationId, before, through)
            is Result.Error -> sender
        }
    }

    private suspend fun loadPages(
        viewerId: SenderId,
        conversationId: ConversationId,
        before: MessageCursor?,
        through: MessageCursor?
    ): Result<MessagePage> {
        var cursor = before
        val messages = mutableListOf<Message>()
        do {
            val page = when (val result = repository.getMessagePage(viewerId, conversationId, cursor)) {
                is Result.Error -> return result
                is Result.Success -> result.data
            }
            messages.addAll(0, page.messages)
            val next = page.olderCursor
            val reachedOldest = through?.let { target -> next?.isAtOrBefore(target) } ?: true
            if (next == null || next == cursor || reachedOldest) {
                return Result.Success(MessagePage(messages, next))
            }
            cursor = next
        } while (true)
    }
    private fun MessageCursor.isAtOrBefore(other: MessageCursor): Boolean =
        createdAt < other.createdAt || (createdAt == other.createdAt && id.value <= other.id.value)
}

class MarkConversationRead(
    private val repository: MessagesRepository,
    private val currentSenderProvider: CurrentMessageSenderProvider
) {
    suspend operator fun invoke(conversationId: ConversationId): Result<Instant> {
        val sender = when (val result = currentSenderProvider.getCurrentSender()) {
            is Result.Success -> result.data
            is Result.Error -> return result
        }
        return repository.markConversationRead(sender.id, conversationId)
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
