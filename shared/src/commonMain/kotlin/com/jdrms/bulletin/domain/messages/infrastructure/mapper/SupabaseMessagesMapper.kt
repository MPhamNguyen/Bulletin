package com.jdrms.bulletin.domain.messages.infrastructure.mapper

import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.service.MessagesPolicy
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationInboxDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationParticipantDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageInsertDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageReportInsertDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageReportType
import kotlin.time.Instant

object SupabaseMessagesMapper {
    fun toConversation(
        conversation: SupabaseConversationDto,
        participants: List<SupabaseConversationParticipantDto>,
        lastMessage: SupabaseMessageDto?,
        historyVisibleAfter: Instant? = null
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
            updatedAtMillis = updatedAtMillis,
            historyVisibleAfter = historyVisibleAfter
        )
    }

    fun toInboxConversation(dto: SupabaseConversationInboxDto): Conversation {
        val cutoff = dto.membership.singleOrNull()?.historyVisibleAfter?.let(Instant::parse)
        val lastMessage = dto.latestMessages.firstOrNull()?.let(::toMessage)?.takeIf { message ->
            cutoff == null || message.orderingTimestamp > cutoff
        }
        val updatedAtMillis = maxOf(
            dto.updatedAt.toEpochMillisecondsOrZero(),
            lastMessage?.timestampMillis ?: 0L
        )
        return Conversation(
            id = ConversationId(dto.id),
            participants = dto.participants.map { participant ->
                ConversationParticipant(
                    id = SenderId(participant.userId),
                    displayName = participant.profile?.fullName?.takeIf(String::isNotBlank) ?: DEFAULT_DISPLAY_NAME
                )
            },
            listingId = dto.listingId?.let(::ListingReferenceId),
            lastMessage = lastMessage,
            updatedAtMillis = updatedAtMillis,
            historyVisibleAfter = cutoff
        )
    }

    fun toParticipant(dto: SupabaseConversationParticipantDto): ConversationParticipant {
        return ConversationParticipant(
            id = SenderId(dto.userId),
            displayName = dto.profile?.fullName?.takeIf(String::isNotBlank) ?: DEFAULT_DISPLAY_NAME
        )
    }

    fun toMessage(dto: SupabaseMessageDto): Message {
        val createdAt = Instant.parse(dto.createdAt)
        return Message(
            id = MessageId(dto.id),
            conversationId = ConversationId(dto.conversationId),
            senderId = SenderId(dto.senderId),
            senderName = dto.senderProfile?.fullName?.takeIf(String::isNotBlank) ?: DEFAULT_DISPLAY_NAME,
            content = dto.content,
            timestampMillis = createdAt.toEpochMilliseconds(),
            isReported = dto.reports.isNotEmpty(),
            preciseCreatedAt = createdAt.takeUnless { it == Instant.fromEpochMilliseconds(it.toEpochMilliseconds()) }
        )
    }

    fun toInsertDto(message: Message): SupabaseMessageInsertDto {
        return SupabaseMessageInsertDto(
            id = message.id.value,
            conversationId = message.conversationId.value,
            content = message.content
        )
    }

    fun toReportInsertDto(
        messageId: MessageId,
        reporterId: SenderId
    ): SupabaseMessageReportInsertDto {
        // Message-report reason selection is not implemented; persist the agreed placeholder for now.
        return SupabaseMessageReportInsertDto(
            messageId = messageId.value,
            reporterId = reporterId.value,
            reason = MessagesPolicy.DEFAULT_REPORT_REASON,
            type = SupabaseMessageReportType.MESSAGE,
            description = null
        )
    }

    private fun String?.toEpochMillisecondsOrZero(): Long {
        return this?.let { timestamp -> runCatching { Instant.parse(timestamp).toEpochMilliseconds() }.getOrNull() }
            ?: 0L
    }

    private const val DEFAULT_DISPLAY_NAME = "Student"
}
