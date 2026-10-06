package com.jdrms.bulletin.domain.messages.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationMembershipDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationParticipantDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageInsertDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageReportInsertDto
import com.jdrms.bulletin.domain.messages.infrastructure.mapper.SupabaseMessagesMapper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.CancellationException

class SupabaseMessagesRepository internal constructor(
    private val messagesTable: SupabaseMessagesTable
) : MessagesRepository {
    constructor(supabase: SupabaseClient) : this(PostgrestSupabaseMessagesTable(supabase))

    override suspend fun getConversations(userId: SenderId): Result<List<Conversation>> = repositoryCall {
        messagesTable.getConversationIds(userId.value)
            .map { conversationId -> loadConversation(conversationId) }
            .sortedByDescending(Conversation::updatedAtMillis)
    }

    override suspend fun getMessages(
        userId: SenderId,
        conversationId: ConversationId
    ): Result<List<Message>> = repositoryCall {
        requireParticipant(userId, conversationId)
        messagesTable.getMessages(conversationId.value).map(SupabaseMessagesMapper::toMessage)
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
        messagesTable.insertReport(
            SupabaseMessagesMapper.toReportInsertDto(messageId, authenticatedReporterId, reason)
        )
    }

    private suspend fun loadConversation(conversationId: String): Conversation {
        val conversation = messagesTable.findConversation(conversationId) ?: throw ConversationAccessException()
        val participants = messagesTable.getParticipants(conversationId)
        val lastMessage = messagesTable.findLatestMessage(conversationId)
        return SupabaseMessagesMapper.toConversation(conversation, participants, lastMessage)
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
        } catch (exception: Throwable) {
            Result.Error(MessagesPersistenceException(exception))
        }
    }

    companion object {
        const val CONVERSATIONS_TABLE = "conversations"
        const val PARTICIPANTS_TABLE = "conversation_participants"
        const val MESSAGES_TABLE = "messages"
        const val REPORTS_TABLE = "message_reports"
    }
}

class MessagesPersistenceException(cause: Throwable) :
    IllegalStateException("Unable to access messages. Please try again.", cause)

internal interface SupabaseMessagesTable {
    suspend fun getAuthenticatedUserId(): String?
    suspend fun getConversationIds(userId: String): List<String>
    suspend fun findConversation(conversationId: String): SupabaseConversationDto?
    suspend fun isParticipant(conversationId: String, userId: String): Boolean
    suspend fun getParticipants(conversationId: String): List<SupabaseConversationParticipantDto>
    suspend fun getMessages(conversationId: String): List<SupabaseMessageDto>
    suspend fun findLatestMessage(conversationId: String): SupabaseMessageDto?
    suspend fun findMessage(conversationId: String, messageId: String): SupabaseMessageDto?
    suspend fun insertMessage(message: SupabaseMessageInsertDto)
    suspend fun insertReport(report: SupabaseMessageReportInsertDto)
}

private class PostgrestSupabaseMessagesTable(
    private val supabase: SupabaseClient
) : SupabaseMessagesTable {
    override suspend fun getAuthenticatedUserId(): String? {
        supabase.auth.awaitInitialization()
        return supabase.auth.currentUserOrNull()?.id
    }

    override suspend fun getConversationIds(userId: String): List<String> {
        return supabase.from(SupabaseMessagesRepository.PARTICIPANTS_TABLE)
            .select(columns = MEMBERSHIP_COLUMNS) {
                filter { eq("user_id", userId) }
            }
            .decodeList<SupabaseConversationMembershipDto>()
            .map(SupabaseConversationMembershipDto::conversationId)
            .distinct()
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

    override suspend fun getMessages(conversationId: String): List<SupabaseMessageDto> {
        return supabase.from(SupabaseMessagesRepository.MESSAGES_TABLE)
            .select(columns = MESSAGE_COLUMNS) {
                filter { eq("conversation_id", conversationId) }
                order("created_at", Order.ASCENDING)
                order("id", Order.ASCENDING)
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
        val MESSAGE_COLUMNS = Columns.raw(
            "id,conversation_id,sender_id,body,created_at,profiles(full_name),message_reports(id)"
        )
    }
}
