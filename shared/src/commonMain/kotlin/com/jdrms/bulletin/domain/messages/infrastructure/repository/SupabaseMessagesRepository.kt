package com.jdrms.bulletin.domain.messages.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.MessagesInstrumentation
import com.jdrms.bulletin.domain.messages.application.NoOpMessagesInstrumentation
import com.jdrms.bulletin.domain.messages.application.measuredMessagesOperation
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageCursor
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.MessagePage
import com.jdrms.bulletin.domain.messages.domain.model.MessageReportAlreadySubmittedException
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationInboxDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationLookupDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationMembershipDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationParticipantDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageInsertDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageReportInsertDto
import com.jdrms.bulletin.domain.messages.infrastructure.mapper.SupabaseMessagesMapper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.time.Instant

class SupabaseMessagesRepository internal constructor(
    private val messagesTable: SupabaseMessagesTable,
    private val instrumentation: MessagesInstrumentation = NoOpMessagesInstrumentation
) : MessagesRepository {
    constructor(
        supabase: SupabaseClient,
        instrumentation: MessagesInstrumentation = NoOpMessagesInstrumentation
    ) : this(PostgrestSupabaseMessagesTable(supabase), instrumentation)

    override suspend fun getConversations(userId: SenderId): Result<List<Conversation>> =
        measuredMessagesOperation(
            instrumentation,
            "inbox_retrieval",
            { result -> (result as? Result.Success)?.data?.size },
            { result -> (result as? Result.Success)?.data?.let { 1 } }
        ) {
            repositoryCall {
                messagesTable.getInboxConversations(userId.value)
                    .distinctBy(SupabaseConversationInboxDto::id)
                    .map(SupabaseMessagesMapper::toInboxConversation)
                    .sortedByDescending(Conversation::updatedAtMillis)
            }
        }

    override suspend fun hideConversation(userId: SenderId, conversationId: ConversationId): Result<Unit> =
        repositoryCall {
            val viewerId = requireAuthenticatedUser(userId)
            val conversation = messagesTable.findConversation(conversationId.value)
                ?: throw ConversationAccessException()
            val participants = messagesTable.getParticipants(conversationId.value)
            SupabaseMessagesMapper.toConversation(conversation, participants, null)
                .authorizeHiding(viewerId).getOrThrow()
            if (!messagesTable.hideConversation(conversationId.value)) throw ConversationAccessException()
        }

    override suspend fun getOrCreateConversation(
        requesterId: SenderId,
        otherParticipant: ConversationParticipant,
        listingId: ListingReferenceId
    ): Result<Conversation> = repositoryCall {
        val authenticatedRequester = requireAuthenticatedUser(requesterId)
        val conversationId = messagesTable.getOrCreateConversation(
            authenticatedRequester.value,
            otherParticipant.id.value,
            listingId.value
        )
        if (!messagesTable.restoreConversation(conversationId)) throw ConversationAccessException()
        loadConversation(conversationId)
    }

    override suspend fun getMessagePage(
        userId: SenderId,
        conversationId: ConversationId,
        before: MessageCursor?,
        limit: Int
    ): Result<MessagePage> = repositoryCall {
        require(limit > 0)
        requireParticipant(userId, conversationId)
        measuredMessagesOperation(instrumentation, "message_fetch", { it.messages.size }) {
            val rows = messagesTable.getMessagePage(conversationId.value, before, limit + 1)
            val visible = rows.take(limit)
            MessagePage(
                messages = visible.map(SupabaseMessagesMapper::toMessage).reversed(),
                olderCursor = visible.lastOrNull()?.takeIf { rows.size > limit }?.let {
                    MessageCursor(Instant.parse(it.createdAt), MessageId(it.id))
                }
            )
        }
    }

    override suspend fun sendMessage(message: Message): Result<Message> = repositoryCall {
        val authenticatedSenderId = requireAuthenticatedUser(message.senderId)
        requireParticipant(authenticatedSenderId, message.conversationId)
        messagesTable.insertMessage(SupabaseMessagesMapper.toInsertDto(message))
        message
    }

    override suspend fun reportMessage(
        userId: SenderId,
        conversationId: ConversationId,
        messageId: MessageId,
        reason: String
    ): Result<Unit> = repositoryCall {
        val authenticatedReporterId = requireAuthenticatedUser(userId)
        requireParticipant(authenticatedReporterId, conversationId)
        if (messagesTable.findMessage(conversationId.value, messageId.value) == null) {
            throw ConversationAccessException()
        }
        submitReport(messageId, authenticatedReporterId)
    }

    private suspend fun submitReport(messageId: MessageId, reporterId: SenderId) {
        try {
            messagesTable.insertReport(SupabaseMessagesMapper.toReportInsertDto(messageId, reporterId))
        } catch (exception: PostgrestRestException) {
            if (exception.isDuplicateMessageReport()) throw MessageReportAlreadySubmittedException()
            throw exception
        }
    }

    private fun PostgrestRestException.isDuplicateMessageReport(): Boolean =
        code == UNIQUE_VIOLATION_CODE && message?.contains(MESSAGE_REPORT_UNIQUE_CONSTRAINT) == true

    private suspend fun loadConversation(conversationId: String): Conversation {
        val (conversation, participants, lastMessage) = coroutineScope {
            val conversation = async { messagesTable.findConversation(conversationId) }
            val participants = async { messagesTable.getParticipants(conversationId) }
            val lastMessage = async { messagesTable.findLatestMessage(conversationId) }
            Triple(conversation.await(), participants.await(), lastMessage.await())
        }
        val accessibleConversation = conversation ?: throw ConversationAccessException()
        return SupabaseMessagesMapper.toConversation(accessibleConversation, participants, lastMessage)
    }

    private suspend fun requireParticipant(userId: SenderId, conversationId: ConversationId) {
        if (!messagesTable.isParticipant(conversationId.value, userId.value)) {
            throw ConversationAccessException()
        }
    }

    private suspend fun requireAuthenticatedUser(claimedUserId: SenderId): SenderId {
        val authenticatedUserId = messagesTable.getAuthenticatedUserId()
            ?.takeIf(String::isNotBlank)
            ?.let(::SenderId)
            ?: throw ConversationAccessException()
        if (authenticatedUserId != claimedUserId) {
            throw ConversationAccessException()
        }
        return authenticatedUserId
    }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun <T> repositoryCall(block: suspend () -> T): Result<T> {
        return try {
            Result.Success(block())
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: ConversationAccessException) {
            Result.Error(exception)
        } catch (exception: MessageReportAlreadySubmittedException) {
            Result.Error(exception)
        } catch (exception: Throwable) {
            Result.Error(MessagesPersistenceException(exception))
        }
    }

    companion object {
        const val CONVERSATIONS_TABLE = "conversations"
        const val PARTICIPANTS_TABLE = "conversation_participants"
        const val MESSAGES_TABLE = "messages"
        const val REPORTS_TABLE = "reports"
        private const val UNIQUE_VIOLATION_CODE = "23505"
        private const val MESSAGE_REPORT_UNIQUE_CONSTRAINT = "reports_active_message_unique"
    }
}

class MessagesPersistenceException(cause: Throwable) :
    IllegalStateException("Unable to access messages. Please try again.", cause)

internal interface SupabaseConversationCreationTable {
    suspend fun getOrCreateConversation(requesterId: String, sellerId: String, listingId: String): String
}

internal interface SupabaseConversationVisibilityTable {
    suspend fun hideConversation(conversationId: String): Boolean
    suspend fun restoreConversation(conversationId: String): Boolean
}

internal interface SupabaseMessagesTable : SupabaseConversationCreationTable, SupabaseConversationVisibilityTable {
    suspend fun getAuthenticatedUserId(): String?
    suspend fun getInboxConversations(userId: String): List<SupabaseConversationInboxDto>
    suspend fun findConversation(conversationId: String): SupabaseConversationDto?
    suspend fun isParticipant(conversationId: String, userId: String): Boolean
    suspend fun getParticipants(conversationId: String): List<SupabaseConversationParticipantDto>
    suspend fun getMessagePage(conversationId: String, before: MessageCursor?, limit: Int): List<SupabaseMessageDto>
    suspend fun findLatestMessage(conversationId: String): SupabaseMessageDto?
    suspend fun findMessage(conversationId: String, messageId: String): SupabaseMessageDto?
    suspend fun insertMessage(message: SupabaseMessageInsertDto)
    suspend fun insertReport(report: SupabaseMessageReportInsertDto)
}

internal class PostgrestSupabaseMessagesTable(
    private val supabase: SupabaseClient
) : SupabaseMessagesTable {
    override suspend fun getAuthenticatedUserId(): String? {
        supabase.auth.awaitInitialization()
        return supabase.auth.currentUserOrNull()?.id
    }

    override suspend fun getInboxConversations(userId: String): List<SupabaseConversationInboxDto> {
        return supabase.from(SupabaseMessagesRepository.CONVERSATIONS_TABLE).select(columns = INBOX_COLUMNS) {
            filter { eq("membership.user_id", userId) }
            order("user_id", Order.ASCENDING, referencedTable = "participants")
            order("created_at", Order.DESCENDING, referencedTable = "messages")
            order("id", Order.DESCENDING, referencedTable = "messages")
            limit(1, referencedTable = "messages")
        }.decodeList()
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

    override suspend fun isParticipant(conversationId: String, userId: String): Boolean {
        return supabase.from(SupabaseMessagesRepository.PARTICIPANTS_TABLE)
            .select(columns = MEMBERSHIP_COLUMNS) {
                filter {
                    eq("conversation_id", conversationId)
                    eq("user_id", userId)
                }
                limit(1)
            }
            .decodeList<SupabaseConversationMembershipDto>()
            .isNotEmpty()
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
        limit: Int
    ): List<SupabaseMessageDto> {
        return supabase.from(SupabaseMessagesRepository.MESSAGES_TABLE)
            .select(columns = MESSAGE_COLUMNS) {
                filter {
                    eq("conversation_id", conversationId)
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
        val MEMBERSHIP_COLUMNS = Columns.raw("conversation_id")
        val PARTICIPANT_COLUMNS = Columns.raw("conversation_id,user_id,profiles(full_name)")
        val INBOX_COLUMNS = Columns.raw(
            "id,updated_at,listing_id,membership:conversation_participants!inner(user_id)," +
                "participants:conversation_participants(user_id,profiles(full_name))," +
                "messages(id,conversation_id,sender_id,body,created_at,profiles(full_name),reports(id,status))"
        )
        val MESSAGE_COLUMNS = Columns.raw(
            "id,conversation_id,sender_id,body,created_at,profiles(full_name),reports(id,status)"
        )
    }
}
