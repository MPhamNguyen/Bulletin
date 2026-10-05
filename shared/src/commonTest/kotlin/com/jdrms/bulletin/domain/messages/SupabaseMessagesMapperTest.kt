package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationParticipantDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageProfileDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageReportMarkerDto
import com.jdrms.bulletin.domain.messages.infrastructure.mapper.SupabaseMessagesMapper
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SupabaseMessagesMapperTest {
    @Test
    fun mapsJoinedProfilesTimestampsAndReportsToDomain() {
        val dto = messageDto(
            profile = SupabaseMessageProfileDto("Alice Student"),
            reports = listOf(SupabaseMessageReportMarkerDto("report-id"))
        )

        val message = SupabaseMessagesMapper.toMessage(dto)

        assertEquals("Alice Student", message.senderName)
        assertEquals(42L, message.timestampMillis)
        assertTrue(message.isReported)
    }

    @Test
    fun missingOrBlankProfileNameUsesSafeDisplayFallback() {
        val participant = SupabaseMessagesMapper.toParticipant(
            participantDto(profile = SupabaseMessageProfileDto("  "))
        )
        val message = SupabaseMessagesMapper.toMessage(messageDto(profile = null))

        assertEquals("Student", participant.displayName)
        assertEquals("Student", message.senderName)
        assertFalse(message.isReported)
    }

    @Test
    fun conversationUsesLatestMessageTimestampWhenBackendSummaryIsStale() {
        val conversation = SupabaseMessagesMapper.toConversation(
            SupabaseConversationDto("conversation-a", "1970-01-01T00:00:00.001Z"),
            listOf(participantDto()),
            messageDto()
        )

        assertEquals(42L, conversation.updatedAtMillis)
        assertEquals(MessageId("message-a"), conversation.lastMessage?.id)
    }

    @Test
    fun insertMappingPreservesContentAndTimestampWithoutClientControlledSender() {
        val insert = SupabaseMessagesMapper.toInsertDto(message())

        assertEquals("message-a", insert.id)
        assertEquals("conversation-a", insert.conversationId)
        assertEquals("Hello", insert.content)
        assertEquals("1970-01-01T00:00:00.042Z", insert.createdAt)
    }

    @Test
    fun translatesDomainContentToSupabaseBodyColumn() {
        val decoded = Json.decodeFromString<SupabaseMessageDto>(
            """
            {
                "id":"message-a",
                "conversation_id":"conversation-a",
                "sender_id":"alice-id",
                "body":"Hello from Supabase",
                "created_at":"1970-01-01T00:00:00.042Z"
            }
            """.trimIndent()
        )
        val encoded = Json.parseToJsonElement(
            Json.encodeToString(SupabaseMessagesMapper.toInsertDto(message()))
        ).jsonObject

        assertEquals("Hello from Supabase", decoded.content)
        assertEquals("Hello", encoded.getValue("body").jsonPrimitive.content)
        assertFalse("content" in encoded)
    }

    @Test
    fun reportMappingPreservesReporterMessageAndReason() {
        val report = SupabaseMessagesMapper.toReportInsertDto(message().id, carol.id, "Spam")

        assertEquals("message-a", report.messageId)
        assertEquals("carol-id", report.reporterId)
        assertEquals("Spam", report.reason)
    }

    private fun participantDto(
        profile: SupabaseMessageProfileDto? = SupabaseMessageProfileDto("Student")
    ) = SupabaseConversationParticipantDto("conversation-a", "alice-id", profile)

    private fun messageDto(
        profile: SupabaseMessageProfileDto? = SupabaseMessageProfileDto("Student"),
        reports: List<SupabaseMessageReportMarkerDto> = emptyList()
    ) = SupabaseMessageDto(
        id = "message-a",
        conversationId = "conversation-a",
        senderId = "alice-id",
        content = "Hello",
        createdAt = "1970-01-01T00:00:00.042Z",
        senderProfile = profile,
        reports = reports
    )
}
