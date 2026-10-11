package com.jdrms.bulletin.domain.messages.infrastructure.mapper

import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageDto
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

object SupabaseRealtimeMessageMapper {
    private val json = Json { ignoreUnknownKeys = true }

    fun toMessage(record: JsonObject, conversationId: ConversationId): Message {
        val dto = json.decodeFromJsonElement<SupabaseMessageDto>(record)
        if (dto.conversationId != conversationId.value) throw ConversationAccessException()
        return SupabaseMessagesMapper.toMessage(dto)
    }
}
