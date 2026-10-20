package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationInboxDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationParticipantDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseInboxMembershipDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseInboxParticipantDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageProfileDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageReportMarkerDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageReportType
import com.jdrms.bulletin.domain.messages.infrastructure.mapper.SupabaseMessagesMapper
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

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
    fun inboxConversationMapsNestedParticipantsAndLatestMessage() {
        val conversation = SupabaseMessagesMapper.toInboxConversation(
            SupabaseConversationInboxDto(
                id = "conversation-a",
                updatedAt = "1970-01-01T00:00:00.001Z",
                participants = listOf(SupabaseInboxParticipantDto("alice-id", SupabaseMessageProfileDto("Alice"))),
                latestMessages = listOf(messageDto())
            )
        )

        assertEquals("Alice", conversation.participants.single().displayName)
        assertEquals(MessageId("message-a"), conversation.lastMessage?.id)
        assertEquals(42L, conversation.updatedAtMillis)
    }

    @Test
    fun inboxConversationExcludesMessageAtViewerCutoff() {
        val cutoff = Instant.fromEpochMilliseconds(42)
        val dto = SupabaseConversationInboxDto(
            id = "conversation-a",
            membership = listOf(SupabaseInboxMembershipDto("alice-id", cutoff.toString())),
            participants = listOf(SupabaseInboxParticipantDto("alice-id")),
            latestMessages = listOf(messageDto())
        )

        val atCutoff = SupabaseMessagesMapper.toInboxConversation(dto)
        val afterCutoff = SupabaseMessagesMapper.toInboxConversation(
            dto.copy(latestMessages = listOf(messageDto().copy(createdAt = "1970-01-01T00:00:00.043Z")))
        )

        assertEquals(cutoff, atCutoff.historyVisibleAfter)
        assertNull(atCutoff.lastMessage)
        assertEquals(MessageId("message-a"), afterCutoff.lastMessage?.id)
    }

    @Test
    fun decodesEmbeddedInboxRowsFromPostgrestShape() {
        val dto = Json.decodeFromString<SupabaseConversationInboxDto>(
            """
            {
                "id":"conversation-a",
                "updated_at":"1970-01-01T00:00:00.001Z",
                "membership":[{"user_id":"alice-id"}],
                "participants":[{"user_id":"alice-id","profiles":{"full_name":"Alice"}}],
                "messages":[{
                    "id":"message-a",
                    "conversation_id":"conversation-a",
                    "sender_id":"alice-id",
                    "body":"Hello",
                    "created_at":"1970-01-01T00:00:00.042Z",
                    "profiles":{"full_name":"Alice"},
                    "reports":[]
                }]
            }
            """.trimIndent()
        )

        assertEquals("Alice", SupabaseMessagesMapper.toInboxConversation(dto).participants.single().displayName)
        assertEquals(MessageId("message-a"), SupabaseMessagesMapper.toInboxConversation(dto).lastMessage?.id)
    }

    @Test
    fun insertMappingLeavesTimestampAndSenderToDatabase() {
        val insert = SupabaseMessagesMapper.toInsertDto(message())

        assertEquals("message-a", insert.id)
        assertEquals("conversation-a", insert.conversationId)
        assertEquals("Hello", insert.content)
        assertFalse(Json.encodeToString(insert).contains("created_at"))
        assertFalse(Json.encodeToString(insert).contains("sender_id"))
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
        assertFalse("sender_id" in encoded)
    }

    @Test
    fun reportMappingUsesStableReasonCodeWithoutDescription() {
        val report = SupabaseMessagesMapper.toReportInsertDto(message().id, carol.id)

        assertEquals("message-a", report.messageId)
        assertEquals("carol-id", report.reporterId)
        assertEquals("inappropriate_content", report.reason)
        assertEquals(SupabaseMessageReportType.MESSAGE, report.type)
        assertNull(report.description)
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
