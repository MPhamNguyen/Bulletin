package com.jdrms.bulletin.domain.messages.infrastructure.repository

import com.jdrms.bulletin.domain.messages.application.MessagesInstrumentation
import com.jdrms.bulletin.domain.messages.application.MessagesMeasurement
import com.jdrms.bulletin.domain.messages.application.measuredMessagesOperation
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationInboxDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator

internal class PostgrestInboxTable(
    private val supabase: SupabaseClient,
    private val instrumentation: MessagesInstrumentation
) : SupabaseInboxTable {
    override suspend fun getInboxConversations(userId: String): List<SupabaseConversationInboxDto> =
        fetchInbox(userId)

    override suspend fun getInboxPage(userId: String, offset: Int, limit: Int) = fetchInbox(userId, offset, limit)

    override suspend fun getInboxConversation(userId: String, conversationId: String) =
        fetchInbox(userId, conversationId = conversationId).firstOrNull()

    private suspend fun fetchInbox(
        userId: String,
        offset: Int = 0,
        pageSize: Int? = null,
        conversationId: String? = null
    ): List<SupabaseConversationInboxDto> {
        val response = measuredMessagesOperation(
            instrumentation,
            "inbox_request_body",
            MessagesMeasurement(responseBytes = { it.data.encodeToByteArray().size })
        ) {
            supabase.from(SupabaseMessagesRepository.CONVERSATIONS_TABLE).select(columns = INBOX_COLUMNS) {
                filter {
                    if (conversationId != null) eq("id", conversationId)
                    eq("membership.user_id", userId)
                    filter("membership.deleted_at", FilterOperator.IS, "null")
                }
                order("updated_at", Order.DESCENDING)
                order("id", Order.DESCENDING)
                if (pageSize != null) range(offset.toLong(), (offset + pageSize - 1).toLong())
                order("user_id", Order.ASCENDING, referencedTable = "participants")
                order("created_at", Order.DESCENDING, referencedTable = "messages")
                order("id", Order.DESCENDING, referencedTable = "messages")
                limit(1, referencedTable = "messages")
            }
        }
        return measuredMessagesOperation(
            instrumentation,
            "inbox_decode",
            MessagesMeasurement(count = { it.size }, callCount = { 0 })
        ) {
            response.decodeList<SupabaseConversationInboxDto>()
        }
    }

    private companion object {
        val INBOX_COLUMNS = Columns.raw(
            "id,updated_at,listing_id,membership:conversation_participants!inner(user_id,history_visible_after)," +
                "participants:conversation_participants(user_id,profiles(full_name),last_read_at)," +
                "messages(id,conversation_id,sender_id,body,created_at,edited_at,deleted_at," +
                "profiles(full_name),reports(id,status))"
        )
    }
}
