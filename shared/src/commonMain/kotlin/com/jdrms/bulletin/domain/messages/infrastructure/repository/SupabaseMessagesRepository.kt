package com.jdrms.bulletin.domain.messages.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.MessagesInstrumentation
import com.jdrms.bulletin.domain.messages.application.MessagesMeasurement
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
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationInboxDto
import com.jdrms.bulletin.domain.messages.infrastructure.mapper.SupabaseMessagesMapper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.time.Instant

class SupabaseMessagesRepository internal constructor(
    private val messagesTable: SupabaseMessagesTable,
    private val instrumentation: MessagesInstrumentation = NoOpMessagesInstrumentation
) : MessagesRepository {
    constructor(
        supabase: SupabaseClient,
        instrumentation: MessagesInstrumentation = NoOpMessagesInstrumentation
    ) : this(PostgrestSupabaseMessagesTable(supabase, instrumentation), instrumentation)

    override suspend fun getConversations(userId: SenderId): Result<List<Conversation>> =
        measuredMessagesOperation(
            instrumentation,
            "inbox_retrieval",
            MessagesMeasurement(
                count = { result -> (result as? Result.Success)?.data?.size },
                callCount = { result -> (result as? Result.Success)?.data?.let { 1 } }
            )
        ) {
            repositoryCall {
                val rows = messagesTable.getInboxConversations(userId.value)
                measuredMessagesOperation(
                    instrumentation,
                    "inbox_mapping",
                    MessagesMeasurement(count = { it.size }, callCount = { 0 })
                ) {
                    rows.distinctBy(SupabaseConversationInboxDto::id)
                        .map(SupabaseMessagesMapper::toInboxConversation)
                        .sortedByDescending(Conversation::updatedAtMillis)
                }
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
        loadConversation(conversationId, authenticatedRequester)
    }

    override suspend fun getMessagePage(
        userId: SenderId,
        conversationId: ConversationId,
        before: MessageCursor?,
        limit: Int
    ): Result<MessagePage> = repositoryCall {
        require(limit > 0)
        val cutoff = requireParticipant(userId, conversationId)
        measuredMessagesOperation(instrumentation, "message_fetch", { it.messages.size }) {
            val rows = messagesTable.getMessagePage(conversationId.value, before, cutoff, limit + 1)
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
        val stored = checkNotNull(messagesTable.findMessage(message.conversationId.value, message.id.value)) {
            "Inserted message was not returned"
        }
        SupabaseMessagesMapper.toMessage(stored)
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

    private suspend fun loadConversation(conversationId: String, viewerId: SenderId): Conversation = coroutineScope {
        val conversation = async { messagesTable.findConversation(conversationId) }
        val participants = async { messagesTable.getParticipants(conversationId) }
        val lastMessage = async { messagesTable.findLatestMessage(conversationId) }
        val membership = async { messagesTable.getMembership(conversationId, viewerId.value) }
        val accessibleConversation = conversation.await() ?: throw ConversationAccessException()
        val cutoff = membership.await()?.historyVisibleAfter?.let(Instant::parse)
        val visibleLastMessage = lastMessage.await()?.takeIf { cutoff == null || Instant.parse(it.createdAt) > cutoff }
        SupabaseMessagesMapper.toConversation(accessibleConversation, participants.await(), visibleLastMessage, cutoff)
    }

    private suspend fun requireParticipant(userId: SenderId, conversationId: ConversationId): Instant? {
        val membership = messagesTable.getMembership(conversationId.value, userId.value)
            ?: throw ConversationAccessException()
        return membership.historyVisibleAfter?.let(Instant::parse)
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
