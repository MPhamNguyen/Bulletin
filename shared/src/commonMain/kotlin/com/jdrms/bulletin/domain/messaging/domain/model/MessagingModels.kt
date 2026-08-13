package com.jdrms.bulletin.domain.messaging.domain.model

import com.jdrms.bulletin.core.common.currentTimeMillis
import kotlin.jvm.JvmInline

@JvmInline
value class ConversationId(val value: String)

@JvmInline
value class MessageId(val value: String)

@JvmInline
value class ParticipantId(val value: String)

data class Message(
    val id: MessageId,
    val conversationId: ConversationId,
    val senderId: ParticipantId,
    val senderName: String,
    val text: String,
    val timestampMillis: Long = currentTimeMillis(),
    val isReported: Boolean = false
)

data class Conversation(
    val id: ConversationId,
    val participantIds: List<ParticipantId>,
    val participantNames: List<String>,
    val lastMessage: Message? = null,
    val updatedAtMillis: Long = currentTimeMillis()
)
