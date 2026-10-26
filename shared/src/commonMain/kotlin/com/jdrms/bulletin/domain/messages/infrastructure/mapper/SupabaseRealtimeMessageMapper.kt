package com.jdrms.bulletin.domain.messages.infrastructure.mapper

import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageDto
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement

object SupabaseRealtimeMessageMapper {
    private val json = Json { ignoreUnknownKeys = true }

    fun toMessage(record: JsonObject, conversationId: ConversationId): Message {
        val dto = json.decodeFromJsonElement<SupabaseMessageDto>(record)
        if (dto.conversationId != conversationId.value) throw ConversationAccessException()
        // Row events omit report joins; unknown must not erase a previously known moderation lock.
        return SupabaseMessagesMapper.toMessage(dto).let { message ->
            if ("reports" in record) message else message.copy(isEditLocked = null)
        }
    }

    fun toMessageOrNull(record: JsonObject, conversationId: ConversationId): Message? = try {
        toMessage(record, conversationId)
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: ConversationAccessException) {
        println("Skipping realtime message for a different conversation.")
        null
    } catch (_: SerializationException) {
        println("Skipping malformed realtime message payload.")
        null
    } catch (_: IllegalArgumentException) {
        println("Skipping realtime message with invalid field values.")
        null
    }
}
