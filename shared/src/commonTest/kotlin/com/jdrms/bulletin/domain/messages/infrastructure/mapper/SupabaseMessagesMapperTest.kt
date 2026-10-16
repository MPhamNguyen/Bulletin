package com.jdrms.bulletin.domain.messages.infrastructure.mapper

import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageDto
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SupabaseMessagesMapperTest {
    @Test
    fun serializedReportContainsOnlyMessageReportFieldsAndExplicitNullDescription() {
        val report = SupabaseMessagesMapper.toReportInsertDto(MessageId("message-id"), SenderId("reporter-id"))

        val payload = Json.parseToJsonElement(Json.encodeToString(report))

        assertEquals(
            Json.parseToJsonElement(
                """{
                    "message_id":"message-id",
                    "reporter_id":"reporter-id",
                    "type":"message",
                    "reason":"Inappropriate Content",
                    "description":null
                }"""
            ),
            payload
        )
    }

    @Test
    fun reportsJoinMarksMessageAsReportedWithoutChangingItsIdentityOrContent() {
        val dto = decodeMessage(""", "reports":[{"id":"report-a"},{"id":"report-b"}]""")

        val message = SupabaseMessagesMapper.toMessage(dto)

        assertTrue(message.isReported)
        assertEquals(MessageId("message-id"), message.id)
        assertEquals(SenderId("sender-id"), message.senderId)
        assertEquals("Original message", message.content)
        assertEquals(42L, message.timestampMillis)
    }

    @Test
    fun missingOrEmptyReportsJoinLeavesMessageUnreported() {
        listOf("", """, "reports":[]""").forEach { reportsField ->
            val message = SupabaseMessagesMapper.toMessage(decodeMessage(reportsField))

            assertFalse(message.isReported)
            assertEquals("Original message", message.content)
        }
    }

    @Test
    fun resolvedAndDismissedReportsKeepTheMessageHidden() {
        listOf("open", "in_review", "resolved", "dismissed").forEach { status ->
            val dto = decodeMessage(""", "reports":[{"id":"report-id","status":"$status"}]""")

            assertTrue(SupabaseMessagesMapper.toMessage(dto).isReported)
        }
    }

    private fun decodeMessage(reportsField: String): SupabaseMessageDto = Json.decodeFromString(
        """{
            "id":"message-id",
            "conversation_id":"conversation-id",
            "sender_id":"sender-id",
            "body":"Original message",
            "created_at":"1970-01-01T00:00:00.042Z"
            $reportsField
        }"""
    )
}
