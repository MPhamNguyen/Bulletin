package com.jdrms.bulletin.domain.messages.infrastructure.mapper

import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SupabaseInboxInvalidationMapperTest {
    @Test
    fun validPayloadCarriesOnlyConversationIdentity() {
        val id = "8e00f862-2e21-4e34-a7a6-a4b39803991e"
        val payload = buildJsonObject {
            put("conversation_id", id)
            put("body", "ignored")
        }
        assertEquals(ConversationId(id), SupabaseInboxInvalidationMapper.conversationIdOrNull(payload))
    }

    @Test
    fun missingOrMalformedConversationIdentityIsIgnored() {
        assertNull(SupabaseInboxInvalidationMapper.conversationIdOrNull(buildJsonObject {}))
        assertNull(
            SupabaseInboxInvalidationMapper.conversationIdOrNull(
                buildJsonObject {
                    put("conversation_id", "not-a-uuid")
                }
            )
        )
    }
}
