package com.jdrms.bulletin.domain.inbox.infrastructure.mapper

import com.jdrms.bulletin.domain.inbox.domain.model.*
import com.jdrms.bulletin.domain.inbox.infrastructure.dto.ConversationDto
import com.jdrms.bulletin.domain.inbox.infrastructure.dto.MessageDto

object MessagingMapper {
    fun toDomain(dto: MessageDto): Message {
        return Message(
            id = MessageId(dto.id),
            conversationId = ConversationId(dto.conversationId),
            senderId = ParticipantId(dto.senderId),
            senderName = dto.senderName,
            text = dto.text,
            timestampMillis = dto.timestampMillis,
            isReported = dto.isReported
        )
    }

    fun toDto(domain: Message): MessageDto {
        return MessageDto(
            id = domain.id.value,
            conversationId = domain.conversationId.value,
            senderId = domain.senderId.value,
            senderName = domain.senderName,
            text = domain.text,
            timestampMillis = domain.timestampMillis,
            isReported = domain.isReported
        )
    }

    fun conversationToDomain(dto: ConversationDto): Conversation {
        return Conversation(
            id = ConversationId(dto.id),
            participantIds = dto.participantIds.map { ParticipantId(it) },
            participantNames = dto.participantNames,
            lastMessage = dto.lastMessage?.let { toDomain(it) },
            updatedAtMillis = dto.updatedAtMillis
        )
    }

    fun conversationToDto(domain: Conversation): ConversationDto {
        return ConversationDto(
            id = domain.id.value,
            participantIds = domain.participantIds.map { it.value },
            participantNames = domain.participantNames,
            lastMessage = domain.lastMessage?.let { toDto(it) },
            updatedAtMillis = domain.updatedAtMillis
        )
    }
}
