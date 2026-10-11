package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import com.jdrms.bulletin.domain.messages.infrastructure.mapper.SupabaseRealtimeMessageMapper
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class SupabaseRealtimeMessageMapperTest {
    @Test
    fun insertPayloadMapsToConversationMessage() {
        val mapped = SupabaseRealtimeMessageMapper.toMessage(record(), conversationId)
        assertEquals(message(), mapped)
    }

    @Test
    fun insertForAnotherConversationIsRejected() {
        assertFailsWith<ConversationAccessException> {
            SupabaseRealtimeMessageMapper.toMessage(record("another"), conversationId)
        }
    }

    @Test
    fun missingRequiredFieldIsRejected() {
        assertFailsWith<Exception> {
            SupabaseRealtimeMessageMapper.toMessage(buildJsonObject { put("id", "missing") }, conversationId)
        }
    }

    @Test
    fun insertForAnotherConversationReturnsNullFromToMessageOrNull() {
        val mapped = SupabaseRealtimeMessageMapper.toMessageOrNull(record("another"), conversationId)
        assertNull(mapped)
    }

    @Test
    fun missingRequiredFieldReturnsNullFromToMessageOrNull() {
        val mapped = SupabaseRealtimeMessageMapper.toMessageOrNull(
            buildJsonObject { put("id", "missing") },
            conversationId
        )
        assertNull(mapped)
    }

    @Test
    fun validPayloadReturnsMessageFromToMessageOrNull() {
        val mapped = SupabaseRealtimeMessageMapper.toMessageOrNull(record(), conversationId)
        assertEquals(message(), mapped)
    }

    private fun record(conversation: String = conversationId.value) = buildJsonObject {
        put("id", "message-a")
        put("conversation_id", conversation)
        put("sender_id", alice.id.value)
        put("body", "Hello")
        put("created_at", "1970-01-01T00:00:00.042Z")
    }
}
