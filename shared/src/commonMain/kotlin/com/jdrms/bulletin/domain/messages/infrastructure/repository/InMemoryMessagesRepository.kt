package com.jdrms.bulletin.domain.messages.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.dto.ConversationDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.MessageDto
import com.jdrms.bulletin.domain.messages.infrastructure.mapper.MessagesMapper
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Development adapter. Conversations are explicitly supplied by tests; no shared fictional inbox is seeded. */
class InMemoryMessagesRepository(
    initialConversations: List<ConversationDto> = emptyList(),
    initialMessages: Map<String, List<MessageDto>> = emptyMap()
) : MessagesRepository {
    private val mutex = Mutex()
    private val conversations = initialConversations.map {
        MessagesMapper.toDomain(it)
    }.associateBy { it.id }.toMutableMap()
    private val messagesByConvId = initialMessages.mapKeys { ConversationId(it.key) }.mapValues { entry ->
        entry.value.map { MessagesMapper.toDomain(it) }.toMutableList()
    }.toMutableMap()

    override suspend fun getConversations(userId: SenderId): Result<List<Conversation>> = mutex.withLock {
        Result.Success(conversations.values.filter { it.includes(userId) })
    }

    override suspend fun getOrCreateConversation(
        requesterId: SenderId,
        otherParticipant: ConversationParticipant,
        listingId: ListingReferenceId
    ): Result<Conversation> = mutex.withLock {
        val existing = conversations.values.firstOrNull { conversation ->
            conversation.listingId == listingId &&
                conversation.includes(requesterId) && conversation.includes(otherParticipant.id)
        }
        if (existing != null) return@withLock Result.Success(existing)
        val conversation = Conversation(
            id = ConversationId("conversation-${conversations.size + 1}"),
            participants = listOf(ConversationParticipant(requesterId, "Student"), otherParticipant),
            listingId = listingId
        )
        conversations[conversation.id] = conversation
        Result.Success(conversation)
    }

    override suspend fun getMessages(
        userId: SenderId,
        conversationId: ConversationId
    ): Result<List<Message>> = mutex.withLock {
        if (conversations[conversationId]?.includes(userId) != true) {
            Result.Error(ConversationAccessException())
        } else {
            Result.Success(messagesByConvId[conversationId]?.toList() ?: emptyList())
        }
    }

    override suspend fun sendMessage(message: Message): Result<Message> = mutex.withLock {
        val conversation = conversations[message.conversationId]
            ?: return@withLock Result.Error(ConversationAccessException())
        when (val updated = conversation.recordMessage(message)) {
            is Result.Error -> updated
            is Result.Success -> {
                messagesByConvId.getOrPut(message.conversationId) { mutableListOf() }.add(message)
                conversations[message.conversationId] = updated.data
                Result.Success(message)
            }
        }
    }

    override suspend fun reportMessage(
        userId: SenderId,
        conversationId: ConversationId,
        messageId: MessageId,
        reason: String
    ): Result<Unit> = mutex.withLock {
        val conversation = conversations[conversationId]
        if (conversation?.includes(userId) != true) {
            return@withLock Result.Error(ConversationAccessException())
        }
        val messages = messagesByConvId[conversationId]
        val index = messages?.indexOfFirst { it.id == messageId } ?: -1
        if (messages == null || index == -1) {
            return@withLock Result.Error(ConversationAccessException())
        }
        val reported = messages[index].report()
        messages[index] = reported
        if (conversation.lastMessage?.id == messageId) {
            conversations[conversationId] = conversation.recordMessage(reported).getOrThrow()
        }
        Result.Success(Unit)
    }
}
