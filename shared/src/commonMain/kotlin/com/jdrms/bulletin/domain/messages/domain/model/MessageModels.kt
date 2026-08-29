package com.jdrms.bulletin.domain.messages.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class ConversationId(val value: String)

@JvmInline
value class MessageId(val value: String)

@JvmInline
value class SenderId(val value: String)

data class Message(
    val id: MessageId,
    val conversationId: ConversationId,
    val senderId: SenderId,
    val senderName: String,
    val content: String,
    val timestampMillis: Long = 0L,
    val isReported: Boolean = false
) {
    init {
        require(content.isNotBlank()) { "Message content cannot be blank." }
        require(content.length <= 1000) { "Message cannot exceed 1000 characters." }
    }
}

data class Conversation(
    val id: ConversationId,
    val participantNames: List<String>,
    val lastMessage: Message? = null,
    val updatedAtMillis: Long = 0L
)
