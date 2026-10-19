package com.jdrms.bulletin.domain.messages.infrastructure.repository

import com.jdrms.bulletin.domain.messages.application.MessagesInstrumentation
import com.jdrms.bulletin.domain.messages.application.MessagesMeasurement
import com.jdrms.bulletin.domain.messages.application.NoOpMessagesInstrumentation
import com.jdrms.bulletin.domain.messages.application.measuredMessagesOperation
import com.jdrms.bulletin.domain.messages.domain.model.MessageCursor
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationInboxDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationLookupDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationMembershipDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationParticipantDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageInsertDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageReportInsertDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Instant

internal interface SupabaseConversationCreationTable {
    suspend fun getOrCreateConversation(requesterId: String, sellerId: String, listingId: String): String
}

internal interface SupabaseConversationVisibilityTable {
    suspend fun hideConversation(conversationId: String): Boolean
    suspend fun restoreConversation(conversationId: String): Boolean
    suspend fun getMembership(conversationId: String, userId: String): SupabaseConversationMembershipDto?
}

internal interface SupabaseMessagesTable : SupabaseConversationCreationTable, SupabaseConversationVisibilityTable {
    suspend fun getAuthenticatedUserId(): String?
    suspend fun getInboxConversations(userId: String): List<SupabaseConversationInboxDto>
    suspend fun findConversation(conversationId: String): SupabaseConversationDto?
    suspend fun isParticipant(conversationId: String, userId: String): Boolean
    suspend fun getParticipants(conversationId: String): List<SupabaseConversationParticipantDto>
    suspend fun getMessagePage(
        conversationId: String,
        before: MessageCursor?,
        visibleAfter: Instant?,
        limit: Int
    ): List<SupabaseMessageDto>
    suspend fun findLatestMessage(conversationId: String): SupabaseMessageDto?
    suspend fun findMessage(conversationId: String, messageId: String): SupabaseMessageDto?
    suspend fun insertMessage(message: SupabaseMessageInsertDto)
    suspend fun insertReport(report: SupabaseMessageReportInsertDto)
}

internal class PostgrestSupabaseMessagesTable(
    private val supabase: SupabaseClient,
    private val instrumentation: MessagesInstrumentation = NoOpMessagesInstrumentation
) : SupabaseMessagesTable {
    override suspend fun getAuthenticatedUserId(): String? {
        supabase.auth.awaitInitialization()
        return supabase.auth.currentUserOrNull()?.id
    }

    override suspend fun getInboxConversations(userId: String): List<SupabaseConversationInboxDto> {
        val response = measuredMessagesOperation(
            instrumentation,
            "inbox_request_body",
            MessagesMeasurement(responseBytes = { it.data.encodeToByteArray().size })
        ) {
            supabase.from(SupabaseMessagesRepository.CONVERSATIONS_TABLE).select(columns = INBOX_COLUMNS) {
                filter {
                    eq("membership.user_id", userId)
                    filter("membership.deleted_at", FilterOperator.IS, "null")
                }
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

    override suspend fun hideConversation(conversationId: String): Boolean =
        supabase.postgrest.rpc(
            "hide_conversation",
            parameters = buildJsonObject { put("p_conversation_id", conversationId) }
        ).decodeAs<Boolean>()

    override suspend fun restoreConversation(conversationId: String): Boolean =
        supabase.postgrest.rpc(
            "restore_conversation",
            parameters = buildJsonObject { put("p_conversation_id", conversationId) }
        ).decodeAs<Boolean>()

    override suspend fun getOrCreateConversation(requesterId: String, sellerId: String, listingId: String): String {
        return supabase.postgrest.rpc(
            "get_or_create_listing_conversation",
            parameters = buildJsonObject {
                put("p_requester_id", requesterId)
                put("p_seller_id", sellerId)
                put("p_listing_id", listingId)
            }
        ).decodeSingle<SupabaseConversationLookupDto>().conversationId
    }

    override suspend fun findConversation(conversationId: String): SupabaseConversationDto? {
        return supabase.from(SupabaseMessagesRepository.CONVERSATIONS_TABLE).select {
            filter { eq("id", conversationId) }
        }.decodeSingleOrNull()
    }

    override suspend fun isParticipant(conversationId: String, userId: String): Boolean =
        getMembership(conversationId, userId) != null

    override suspend fun getMembership(conversationId: String, userId: String): SupabaseConversationMembershipDto? {
        return supabase.from(SupabaseMessagesRepository.PARTICIPANTS_TABLE)
            .select(columns = MEMBERSHIP_COLUMNS) {
                filter {
                    eq("conversation_id", conversationId)
                    eq("user_id", userId)
                }
                limit(1)
            }
            .decodeSingleOrNull()
    }

    override suspend fun getParticipants(conversationId: String): List<SupabaseConversationParticipantDto> {
        return supabase.from(SupabaseMessagesRepository.PARTICIPANTS_TABLE)
            .select(columns = PARTICIPANT_COLUMNS) {
                filter { eq("conversation_id", conversationId) }
                order("user_id", Order.ASCENDING)
            }
            .decodeList()
    }

    override suspend fun getMessagePage(
        conversationId: String,
        before: MessageCursor?,
        visibleAfter: Instant?,
        limit: Int
    ): List<SupabaseMessageDto> {
        return supabase.from(SupabaseMessagesRepository.MESSAGES_TABLE)
            .select(columns = MESSAGE_COLUMNS) {
                filter {
                    eq("conversation_id", conversationId)
                    if (visibleAfter != null) gt("created_at", visibleAfter.toString())
                    if (before != null) {
                        or {
                            lt("created_at", before.createdAt.toString())
                            and {
                                eq("created_at", before.createdAt.toString())
                                lt("id", before.id.value)
                            }
                        }
                    }
                }
                order("created_at", Order.DESCENDING)
                order("id", Order.DESCENDING)
                limit(limit.toLong())
            }
            .decodeList()
    }

    override suspend fun findMessage(conversationId: String, messageId: String): SupabaseMessageDto? {
        return supabase.from(SupabaseMessagesRepository.MESSAGES_TABLE)
            .select(columns = MESSAGE_COLUMNS) {
                filter {
                    eq("conversation_id", conversationId)
                    eq("id", messageId)
                }
            }
            .decodeSingleOrNull()
    }

    override suspend fun findLatestMessage(conversationId: String): SupabaseMessageDto? {
        return supabase.from(SupabaseMessagesRepository.MESSAGES_TABLE)
            .select(columns = MESSAGE_COLUMNS) {
                filter { eq("conversation_id", conversationId) }
                order("created_at", Order.DESCENDING)
                order("id", Order.DESCENDING)
                limit(1)
            }
            .decodeSingleOrNull()
    }

    override suspend fun insertMessage(message: SupabaseMessageInsertDto) {
        supabase.from(SupabaseMessagesRepository.MESSAGES_TABLE).insert(message)
    }

    override suspend fun insertReport(report: SupabaseMessageReportInsertDto) {
        supabase.from(SupabaseMessagesRepository.REPORTS_TABLE).insert(report)
    }

    private companion object {
        val MEMBERSHIP_COLUMNS = Columns.raw("conversation_id,history_visible_after")
        val PARTICIPANT_COLUMNS = Columns.raw("conversation_id,user_id,profiles(full_name)")
        val INBOX_COLUMNS = Columns.raw(
            "id,updated_at,listing_id,membership:conversation_participants!inner(user_id,history_visible_after)," +
                "participants:conversation_participants(user_id,profiles(full_name))," +
                "messages(id,conversation_id,sender_id,body,created_at,profiles(full_name),reports(id,status))"
        )
        val MESSAGE_COLUMNS = Columns.raw(
            "id,conversation_id,sender_id,body,created_at,profiles(full_name),reports(id,status)"
        )
    }
}
