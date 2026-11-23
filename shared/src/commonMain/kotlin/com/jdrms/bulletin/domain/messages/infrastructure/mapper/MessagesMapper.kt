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
            senderId = dto.senderId?.let(::SenderId),
            senderName = if (dto.senderId == null) Message.MISSING_SENDER_NAME else dto.senderName,
            content = dto.content,
            timestampMillis = dto.timestampMillis,
            isReported = dto.isReported,
            editedAt = dto.editedAt,
            deletedAt = dto.deletedAt,
            isEditLocked = dto.isEditLocked
        )
    }

    fun toDto(domain: Message): MessageDto {
        return MessageDto(
            id = domain.id.value,
            conversationId = domain.conversationId.value,
            senderId = domain.senderId?.value,
            senderName = domain.displaySenderName,
            content = domain.content,
            timestampMillis = domain.timestampMillis,
            isReported = domain.isReported,
            editedAt = domain.editedAt,
            deletedAt = domain.deletedAt,
            isEditLocked = domain.isEditLocked
        )
    }

    fun toDomain(dto: ConversationDto): Conversation {
        return Conversation(
            id = ConversationId(dto.id),
            participants = dto.participants.map {
                ConversationParticipant(
                    SenderId(it.id),
                    it.displayName,
                    avatarUrl = it.avatarUrl
                )
            },
            listingId = dto.listingId?.let(::ListingReferenceId),
            lastMessage = dto.lastMessage?.let { toDomain(it) },
            updatedAtMillis = dto.updatedAtMillis,
            isListingSold = dto.isListingSold,
            listingThumbnailUrl = dto.listingThumbnailUrl
        )
    }

    fun toDto(domain: Conversation): ConversationDto {
        return ConversationDto(
            id = domain.id.value,
            participants = domain.participants.map {
                ConversationParticipantDto(
                    it.id.value,
                    it.displayName,
                    it.avatarUrl
                )
            },
            listingId = domain.listingId?.value,
            lastMessage = domain.lastMessage?.let { toDto(it) },
            updatedAtMillis = domain.updatedAtMillis,
            isListingSold = domain.isListingSold,
            listingThumbnailUrl = domain.listingThumbnailUrl
        )
    }
}
