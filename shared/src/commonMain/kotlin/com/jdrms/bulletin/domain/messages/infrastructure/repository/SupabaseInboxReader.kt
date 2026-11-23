package com.jdrms.bulletin.domain.messages.infrastructure.repository

import com.jdrms.bulletin.domain.messages.application.MessagesInstrumentation
import com.jdrms.bulletin.domain.messages.application.MessagesMeasurement
import com.jdrms.bulletin.domain.messages.application.measuredMessagesOperation
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationInboxDto
import com.jdrms.bulletin.domain.messages.infrastructure.mapper.SupabaseMessagesMapper

internal class SupabaseInboxReader(
    private val table: SupabaseInboxTable,
    private val instrumentation: MessagesInstrumentation,
    private val imageUrl: (String) -> String
) {
    suspend fun all(userId: SenderId): List<Conversation> {
        val rows = table.getInboxConversations(userId.value)
        return measuredMessagesOperation(
            instrumentation,
            "inbox_mapping",
            MessagesMeasurement(count = { it.size }, callCount = { 0 })
        ) {
            rows.distinctBy(SupabaseConversationInboxDto::id).map(::map)
                .sortedByDescending(Conversation::updatedAtMillis)
        }
    }

    suspend fun page(userId: SenderId, offset: Int, limit: Int): List<Conversation> {
        require(offset >= 0 && limit > 0)
        return table.getInboxPage(userId.value, offset, limit).map(::map)
    }

    suspend fun conversation(userId: SenderId, id: ConversationId): Conversation? =
        table.getInboxConversation(userId.value, id.value)?.let(::map)

    private fun map(row: SupabaseConversationInboxDto): Conversation =
        SupabaseMessagesMapper.toInboxConversation(row, imageUrl)
}
