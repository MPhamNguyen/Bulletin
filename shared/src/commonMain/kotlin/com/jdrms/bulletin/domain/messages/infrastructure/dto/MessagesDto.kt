package com.jdrms.bulletin.domain.messages.infrastructure.dto

data class MessageDto(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val content: String,
    val timestampMillis: Long = 0L,
    val isReported: Boolean = false
)

data class ConversationDto(
    val id: String,
    val participants: List<ConversationParticipantDto>,
    val lastMessage: MessageDto? = null,
    val updatedAtMillis: Long = 0L
)

data class ConversationParticipantDto(val id: String, val displayName: String)
