package com.jdrms.bulletin.domain.messages.infrastructure.mapper

import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.infrastructure.dto.ConversationDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.ConversationParticipantDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.MessageDto

object MessagesMapper {
    fun toDomain(dto: MessageDto): Message {
        return Message(
            id = MessageId(dto.id),
            conversationId = ConversationId(dto.conversationId),
            senderId = SenderId(dto.senderId),
            senderName = dto.senderName,
            content = dto.content,
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
            content = domain.content,
            timestampMillis = domain.timestampMillis,
            isReported = domain.isReported
        )
    }

    fun toDomain(dto: ConversationDto): Conversation {
        return Conversation(
            id = ConversationId(dto.id),
            participants = dto.participants.map { ConversationParticipant(SenderId(it.id), it.displayName) },
            listingId = dto.listingId?.let(::ListingReferenceId),
            lastMessage = dto.lastMessage?.let { toDomain(it) },
            updatedAtMillis = dto.updatedAtMillis
        )
    }

    fun toDto(domain: Conversation): ConversationDto {
        return ConversationDto(
            id = domain.id.value,
            participants = domain.participants.map { ConversationParticipantDto(it.id.value, it.displayName) },
            listingId = domain.listingId?.value,
            lastMessage = domain.lastMessage?.let { toDto(it) },
            updatedAtMillis = domain.updatedAtMillis
        )
    }
}
