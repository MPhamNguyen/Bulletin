package com.jdrms.bulletin.domain.messages.infrastructure.mapper

import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationParticipantDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageInsertDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageReportInsertDto
import kotlin.time.Instant

object SupabaseMessagesMapper {
    fun toConversation(
        conversation: SupabaseConversationDto,
        participants: List<SupabaseConversationParticipantDto>,
        lastMessage: SupabaseMessageDto?
    ): Conversation {
        val mappedLastMessage = lastMessage?.let(::toMessage)
        val updatedAtMillis = maxOf(
            conversation.updatedAt.toEpochMillisecondsOrZero(),
            mappedLastMessage?.timestampMillis ?: 0L
        )
        return Conversation(
            id = ConversationId(conversation.id),
            participants = participants.map(::toParticipant),
            listingId = conversation.listingId?.let(::ListingReferenceId),
            lastMessage = mappedLastMessage,
            updatedAtMillis = updatedAtMillis
        )
    }

    fun toParticipant(dto: SupabaseConversationParticipantDto): ConversationParticipant {
        return ConversationParticipant(
            id = SenderId(dto.userId),
            displayName = dto.profile?.fullName?.takeIf(String::isNotBlank) ?: DEFAULT_DISPLAY_NAME
        )
    }

    fun toMessage(dto: SupabaseMessageDto): Message {
        return Message(
            id = MessageId(dto.id),
            conversationId = ConversationId(dto.conversationId),
            senderId = SenderId(dto.senderId),
            senderName = dto.senderProfile?.fullName?.takeIf(String::isNotBlank) ?: DEFAULT_DISPLAY_NAME,
            content = dto.content,
            timestampMillis = Instant.parse(dto.createdAt).toEpochMilliseconds(),
            isReported = dto.reports.isNotEmpty()
        )
    }

    fun toInsertDto(message: Message): SupabaseMessageInsertDto {
        return SupabaseMessageInsertDto(
            id = message.id.value,
            conversationId = message.conversationId.value,
            content = message.content,
            createdAt = Instant.fromEpochMilliseconds(message.timestampMillis).toString()
        )
    }

    fun toReportInsertDto(
        messageId: MessageId,
        reporterId: SenderId,
        reason: String
    ): SupabaseMessageReportInsertDto {
        return SupabaseMessageReportInsertDto(
            messageId = messageId.value,
            reporterId = reporterId.value,
            reason = reason
        )
    }

    private fun String?.toEpochMillisecondsOrZero(): Long {
        return this?.let { timestamp -> runCatching { Instant.parse(timestamp).toEpochMilliseconds() }.getOrNull() }
            ?: 0L
    }

    private const val DEFAULT_DISPLAY_NAME = "Student"
}
