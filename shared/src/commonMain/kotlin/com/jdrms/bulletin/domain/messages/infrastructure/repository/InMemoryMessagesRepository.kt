package com.jdrms.bulletin.domain.messages.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.dto.ConversationDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.MessageDto
import com.jdrms.bulletin.domain.messages.infrastructure.mapper.MessagesMapper

class InMemoryMessagesRepository(
    initialConversations: List<ConversationDto> = defaultSeedConversations,
    initialMessages: Map<String, List<MessageDto>> = defaultSeedMessages
) : MessagesRepository {

    private val conversations = initialConversations.map { MessagesMapper.toDomain(it) }.toMutableList()
    private val messagesByConvId = initialMessages.mapValues { entry ->
        entry.value.map { MessagesMapper.toDomain(it) }.toMutableList()
    }.toMutableMap()

    override suspend fun getConversations(userId: String): List<Conversation> {
        return conversations.toList()
    }

    override suspend fun getMessages(conversationId: ConversationId): List<Message> {
        return messagesByConvId[conversationId.value]?.toList() ?: emptyList()
    }

    override suspend fun sendMessage(conversationId: ConversationId, message: Message): Result<Message> {
        val list = messagesByConvId.getOrPut(conversationId.value) { mutableListOf() }
        list.add(message)

        val convIndex = conversations.indexOfFirst { it.id == conversationId }
        if (convIndex != -1) {
            val old = conversations[convIndex]
            conversations[convIndex] = old.copy(lastMessage = message, updatedAtMillis = message.timestampMillis)
        }

        return Result.Success(message)
    }

    override suspend fun reportMessage(messageId: MessageId, reason: String): Result<Unit> {
        messagesByConvId.values.forEach { messageList ->
            val index = messageList.indexOfFirst { it.id == messageId }
            if (index != -1) {
                messageList[index] = messageList[index].copy(isReported = true)
            }
        }
        return Result.Success(Unit)
    }

    companion object {
        private val defaultSeedConversations = listOf(
            ConversationDto(
                id = "conv_1",
                participantNames = listOf("Sean Gallagher"),
                lastMessage = MessageDto(
                    id = "msg_1_2",
                    conversationId = "conv_1",
                    senderId = "seller_102",
                    senderName = "Sean Gallagher",
                    content = "Sounds good! Let meet at the campus library 2nd floor.",
                    timestampMillis = 1000L
                )
            ),
            ConversationDto(
                id = "conv_2",
                participantNames = listOf("Jacob Ayoub"),
                lastMessage = MessageDto(
                    id = "msg_2_1",
                    conversationId = "conv_2",
                    senderId = "seller_103",
                    senderName = "Jacob Ayoub",
                    content = "Hey! Is the mini fridge still available?",
                    timestampMillis = 2000L
                )
            )
        )

        private val defaultSeedMessages = mapOf(
            "conv_1" to listOf(
                MessageDto(
                    id = "msg_1_1",
                    conversationId = "conv_1",
                    senderId = "current_student",
                    senderName = "Dominic Alfonso",
                    content = "Hi Sean, can you do $160 for the Sony headphones?",
                    timestampMillis = 900L
                ),
                MessageDto(
                    id = "msg_1_2",
                    conversationId = "conv_1",
                    senderId = "seller_102",
                    senderName = "Sean Gallagher",
                    content = "Sounds good! Let meet at the campus library 2nd floor.",
                    timestampMillis = 1000L
                )
            ),
            "conv_2" to listOf(
                MessageDto(
                    id = "msg_2_1",
                    conversationId = "conv_2",
                    senderId = "seller_103",
                    senderName = "Jacob Ayoub",
                    content = "Hey! Is the mini fridge still available?",
                    timestampMillis = 2000L
                )
            )
        )
    }
}
