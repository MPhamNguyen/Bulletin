package com.jdrms.bulletin.domain.inbox.infrastructure.dto

data class MessageDto(
    val id: String,
    val conversationId: String,
    val senderId: String,
    val senderName: String,
    val text: String,
    val timestampMillis: Long,
    val isReported: Boolean = false
)

data class ConversationDto(
    val id: String,
    val participantIds: List<String>,
    val participantNames: List<String>,
    val lastMessage: MessageDto? = null,
    val updatedAtMillis: Long
)
