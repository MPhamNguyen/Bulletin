package com.jdrms.bulletin.domain.messages.infrastructure.mapper

import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonPrimitive

internal object SupabaseInboxInvalidationMapper {
    private val uuid = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    fun conversationIdOrNull(payload: JsonObject): ConversationId? {
        val raw = runCatching { payload["conversation_id"]?.jsonPrimitive?.content }.getOrNull()
        return raw?.takeIf(uuid::matches)?.let(::ConversationId)
    }
}
