package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageProfileDto
import com.jdrms.bulletin.domain.messages.infrastructure.mapper.MessagesMapper
import com.jdrms.bulletin.domain.messages.infrastructure.mapper.SupabaseMessagesMapper
import com.jdrms.bulletin.domain.messages.infrastructure.mapper.SupabaseRealtimeMessageMapper
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RetainedMessageMappingTest {
    private val payload = """{
        "id":"retained", "conversation_id":"conversation-a", "sender_id":null,
        "body":"Still here", "created_at":"2026-11-17T12:00:00Z"
    }"""

    @Test
    fun postgrestAndRealtimeDecodeNullSendersAndPreserveContent() {
        val dto = Json.decodeFromString<SupabaseMessageDto>(payload)
        val row = SupabaseMessagesMapper.toMessage(dto)
        val event = SupabaseRealtimeMessageMapper.toMessage(Json.parseToJsonElement(payload).jsonObject, conversationId)
        listOf(row, event).forEach { message ->
            assertNull(message.senderId)
            assertEquals("User Not Found", message.senderName)
            assertEquals("Still here", message.content)
        }
    }

    @Test
    fun staleJoinedOrCachedNamesAreDiscardedForDeletedSenders() {
        val dto = Json.decodeFromString<SupabaseMessageDto>(payload)
        val staleJoin = dto.copy(senderProfile = SupabaseMessageProfileDto("Old personal name"))
        assertEquals(Message.MISSING_SENDER_NAME, SupabaseMessagesMapper.toMessage(staleJoin).senderName)
        val retained = message(carol).copy(senderId = null)
        val cached = MessagesMapper.toDto(retained)
        assertNull(cached.senderId)
        assertEquals(Message.MISSING_SENDER_NAME, cached.senderName)
        assertEquals(
            Message.MISSING_SENDER_NAME,
            MessagesMapper.toDomain(cached.copy(senderName = "Old name")).senderName
        )
    }
}
