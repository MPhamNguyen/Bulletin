package com.jdrms.bulletin.domain.messages.domain.model

import com.jdrms.bulletin.core.common.Result
import kotlin.jvm.JvmInline

@JvmInline
value class ConversationId(val value: String)

@JvmInline
value class MessageId(val value: String)

@JvmInline
value class SenderId(val value: String) {
    init {
        require(value.isNotBlank()) { "Sender ID cannot be blank." }
    }
}

data class ConversationParticipant(val id: SenderId, val displayName: String)

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

    fun report(): Message = copy(isReported = true)
}

class Conversation(
    val id: ConversationId,
    participants: List<ConversationParticipant>,
    val lastMessage: Message? = null,
    val updatedAtMillis: Long = 0L
) {
    val participants: List<ConversationParticipant> = participants.toList()
    val participantNames: List<String> get() = participants.map { it.displayName }

    init {
        require(participants.isNotEmpty()) { "A conversation must have participants." }
        require(participants.map { it.id }.distinct().size == participants.size) { "Participants must be unique." }
        require(lastMessage == null || lastMessage.conversationId == id) { "Message belongs to another conversation." }
        require(lastMessage == null || includes(lastMessage.senderId)) { "Sender must be a participant." }
    }

    fun includes(senderId: SenderId): Boolean = participants.any { it.id == senderId }

    fun recordMessage(message: Message): Result<Conversation> {
        return if (message.conversationId != id || !includes(message.senderId)) {
            Result.Error(ConversationAccessException())
        } else {
            Result.Success(Conversation(id, participants, message, message.timestampMillis))
        }
    }
}

class ConversationAccessException : IllegalStateException("Conversation is unavailable for this user.")
