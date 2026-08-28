package com.jdrms.bulletin.domain.inbox.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.common.currentTimeMillis
import com.jdrms.bulletin.core.common.generateUuid
import com.jdrms.bulletin.domain.inbox.domain.model.*
import com.jdrms.bulletin.domain.inbox.domain.repository.MessagingRepository
import com.jdrms.bulletin.domain.inbox.infrastructure.dto.ConversationDto
import com.jdrms.bulletin.domain.inbox.infrastructure.dto.MessageDto
import com.jdrms.bulletin.domain.inbox.infrastructure.mapper.MessagingMapper

/**
 * In-memory [MessagingRepository] with a seeded mock conversation, for development before the
 * Supabase-backed implementation is wired up.
 */
class InMemoryMessagingRepository : MessagingRepository {

    private val conversationsMap = mutableMapOf<String, ConversationDto>(
        "conv_1" to ConversationDto(
            id = "conv_1",
            participantIds = listOf("user_101", "user_102"),
            participantNames = listOf("Dominic Alfonso", "Sean Gallagher"),
            lastMessage = MessageDto(
                id = "msg_2",
                conversationId = "conv_1",
                senderId = "user_102",
                senderName = "Sean Gallagher",
                text = "Hey! Is the CECS 491 textbook still available?",
                timestampMillis = 1723512000000L
            ),
            updatedAtMillis = 1723512000000L
        )
    )

    private val messagesMap = mutableMapOf<String, MutableList<MessageDto>>(
        "conv_1" to mutableListOf(
            MessageDto(
                id = "msg_1",
                conversationId = "conv_1",
                senderId = "user_101",
                senderName = "Dominic Alfonso",
                text = "Hi Sean!",
                timestampMillis = 1723511000000L
            ),
            MessageDto(
                id = "msg_2",
                conversationId = "conv_1",
                senderId = "user_102",
                senderName = "Sean Gallagher",
                text = "Hey! Is the CECS 491 textbook still available?",
                timestampMillis = 1723512000000L
            )
        )
    )

    override suspend fun getConversations(userId: ParticipantId): List<Conversation> {
        return conversationsMap.values
            .filter { it.participantIds.contains(userId.value) }
            .map { MessagingMapper.conversationToDomain(it) }
    }

    override suspend fun getMessages(conversationId: ConversationId): List<Message> {
        return messagesMap[conversationId.value]?.map { MessagingMapper.toDomain(it) } ?: emptyList()
    }

    override suspend fun sendMessage(
        conversationId: ConversationId,
        senderId: ParticipantId,
        senderName: String,
        text: String
    ): Result<Message> {
        val msgDto = MessageDto(
            id = "msg_" + generateUuid(),
            conversationId = conversationId.value,
            senderId = senderId.value,
            senderName = senderName,
            text = text,
            timestampMillis = currentTimeMillis()
        )
        val list = messagesMap.getOrPut(conversationId.value) { mutableListOf() }
        list.add(msgDto)

        val conv = conversationsMap[conversationId.value]
        if (conv != null) {
            conversationsMap[conversationId.value] = conv.copy(
                lastMessage = msgDto,
                updatedAtMillis = msgDto.timestampMillis
            )
        }

        return Result.Success(MessagingMapper.toDomain(msgDto))
    }

    override suspend fun reportMessage(messageId: MessageId, reason: String): Result<Unit> {
        for (list in messagesMap.values) {
            val index = list.indexOfFirst { it.id == messageId.value }
            if (index != -1) {
                list[index] = list[index].copy(isReported = true)
                break
            }
        }
        return Result.Success(Unit)
    }

    override suspend fun startConversation(
        participant1: ParticipantId,
        name1: String,
        participant2: ParticipantId,
        name2: String
    ): Result<Conversation> {
        val convId = "conv_" + generateUuid()
        val convDto = ConversationDto(
            id = convId,
            participantIds = listOf(participant1.value, participant2.value),
            participantNames = listOf(name1, name2),
            updatedAtMillis = currentTimeMillis()
        )
        conversationsMap[convId] = convDto
        messagesMap[convId] = mutableListOf()
        return Result.Success(MessagingMapper.conversationToDomain(convDto))
    }
}
