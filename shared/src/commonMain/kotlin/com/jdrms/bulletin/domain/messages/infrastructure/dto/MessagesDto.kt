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
    val participantNames: List<String>,
    val lastMessage: MessageDto? = null,
    val updatedAtMillis: Long = 0L
)
