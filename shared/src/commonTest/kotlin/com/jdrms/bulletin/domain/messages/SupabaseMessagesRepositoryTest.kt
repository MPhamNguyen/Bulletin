package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationParticipantDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageInsertDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageProfileDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageReportInsertDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageReportMarkerDto
import com.jdrms.bulletin.domain.messages.infrastructure.repository.MessagesPersistenceException
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesTable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.time.Instant

class SupabaseMessagesRepositoryContractTest : MessagesRepositoryContractTest() {
    override fun createRepository(conversations: List<Conversation>, messages: List<Message>): MessagesRepository {
        return SupabaseMessagesRepository(FakeSupabaseMessagesTable(conversations, messages))
    }
}

class SupabaseMessagesRepositoryTest {
    @Test
    fun sendWritesTransportFieldsWithoutClientControlledIdentity() = runTest {
        val table = FakeSupabaseMessagesTable(listOf(conversation()))
        val repository = SupabaseMessagesRepository(table)

        assertEquals(Result.Success(message()), repository.sendMessage(message()))

        assertEquals(
            SupabaseMessageInsertDto(
                id = "message-a",
                conversationId = "conversation-a",
                content = "Hello",
                createdAt = "1970-01-01T00:00:00.042Z"
            ),
            table.insertedMessage
        )
    }

    @Test
    fun authenticatedUserCannotSendMessageClaimingToBeAnotherParticipant() = runTest {
        val table = FakeSupabaseMessagesTable(
            conversations = listOf(conversation()),
            authenticatedUserId = alice.id.value
        )
        val repository = SupabaseMessagesRepository(table)

        val result = repository.sendMessage(message(carol))

        assertIs<ConversationAccessException>(assertIs<Result.Error>(result).exception)
        assertNull(table.insertedMessage)
    }

    @Test
    fun sendRequiresAnAuthenticatedSession() = runTest {
        val table = FakeSupabaseMessagesTable(
            conversations = listOf(conversation()),
            authenticatedUserId = null
        )

        val result = SupabaseMessagesRepository(table).sendMessage(message())

        assertIs<ConversationAccessException>(assertIs<Result.Error>(result).exception)
        assertNull(table.insertedMessage)
    }

    @Test
    fun reportWritesAuthenticatedReporterAndReason() = runTest {
        val table = FakeSupabaseMessagesTable(
            conversations = listOf(conversation()),
            messages = listOf(message()),
            authenticatedUserId = carol.id.value
        )
        val repository = SupabaseMessagesRepository(table)

        repository.reportMessage(carol.id, conversationId, message().id, "Harassment").getOrThrow()

        assertEquals(
            SupabaseMessageReportInsertDto("message-a", "carol-id", "Harassment"),
            table.insertedReport
        )
    }

    @Test
    fun authenticatedUserCannotReportAsAnotherParticipant() = runTest {
        val table = FakeSupabaseMessagesTable(
            conversations = listOf(conversation()),
            messages = listOf(message()),
            authenticatedUserId = alice.id.value
        )

        val result = SupabaseMessagesRepository(table).reportMessage(
            carol.id,
            conversationId,
            message().id,
            "Harassment"
        )

        assertIs<ConversationAccessException>(assertIs<Result.Error>(result).exception)
        assertNull(table.insertedReport)
    }

    @Test
    fun reportRequiresAnAuthenticatedSession() = runTest {
        val table = FakeSupabaseMessagesTable(
            conversations = listOf(conversation()),
            messages = listOf(message()),
            authenticatedUserId = null
        )

        val result = SupabaseMessagesRepository(table).reportMessage(
            alice.id,
            conversationId,
            message().id,
            "Harassment"
        )

        assertIs<ConversationAccessException>(assertIs<Result.Error>(result).exception)
        assertNull(table.insertedReport)
    }

    @Test
    fun unexpectedStorageFailureReturnsSafeErrorAndPreservesCause() = runTest {
        val cause = IllegalStateException("PostgREST private detail")
        val table = object : SupabaseMessagesTable by FakeSupabaseMessagesTable(emptyList()) {
            override suspend fun getConversationIds(userId: String): List<String> = throw cause
        }

        val result = assertIs<Result.Error>(SupabaseMessagesRepository(table).getConversations(alice.id))

        assertEquals("Unable to access messages. Please try again.", result.message)
        assertIs<MessagesPersistenceException>(result.exception)
        assertSame(cause, result.exception.cause)
    }

    @Test
    fun cancellationFromStorageIsRethrown() = runTest {
        val table = object : SupabaseMessagesTable by FakeSupabaseMessagesTable(emptyList()) {
            override suspend fun getConversationIds(userId: String): List<String> {
                throw CancellationException("Cancelled")
            }
        }

        assertFailsWith<CancellationException> {
            SupabaseMessagesRepository(table).getConversations(alice.id)
        }
    }

    @Test
    fun malformedBackendTimestampReturnsPersistenceFailure() = runTest {
        val table = FakeSupabaseMessagesTable(listOf(conversation()), listOf(message())).apply {
            replaceMessageTimestamp("not-an-instant")
        }

        val result = SupabaseMessagesRepository(table).getMessages(alice.id, conversationId)

        assertIs<MessagesPersistenceException>(assertIs<Result.Error>(result).exception)
    }
}

private class FakeSupabaseMessagesTable(
    conversations: List<Conversation>,
    messages: List<Message> = emptyList(),
    private val authenticatedUserId: String? = alice.id.value
) : SupabaseMessagesTable {
    private val conversationRows = conversations.associate { conversation ->
        conversation.id.value to SupabaseConversationDto(
            id = conversation.id.value,
            updatedAt = Instant.fromEpochMilliseconds(conversation.updatedAtMillis).toString()
        )
    }.toMutableMap()
    private val participantRows = conversations.flatMap { conversation ->
        conversation.participants.map { participant ->
            SupabaseConversationParticipantDto(
                conversationId = conversation.id.value,
                userId = participant.id.value,
                profile = SupabaseMessageProfileDto(participant.displayName)
            )
        }
    }.toMutableList()
    private val messageRows = messages.map(::toMessageDto).toMutableList()

    var insertedMessage: SupabaseMessageInsertDto? = null
    var insertedReport: SupabaseMessageReportInsertDto? = null

    override suspend fun getAuthenticatedUserId(): String? = authenticatedUserId

    override suspend fun getConversationIds(userId: String): List<String> {
        return participantRows.filter { it.userId == userId }.map { it.conversationId }.distinct()
    }

    override suspend fun findConversation(conversationId: String): SupabaseConversationDto? {
        return conversationRows[conversationId]
    }

    override suspend fun isParticipant(conversationId: String, userId: String): Boolean {
        return participantRows.any { it.conversationId == conversationId && it.userId == userId }
    }

    override suspend fun getParticipants(conversationId: String): List<SupabaseConversationParticipantDto> {
        return participantRows.filter { it.conversationId == conversationId }
    }

    override suspend fun getMessages(conversationId: String): List<SupabaseMessageDto> {
        return messageRows.filter { it.conversationId == conversationId }
            .sortedWith(compareBy(SupabaseMessageDto::createdAt, SupabaseMessageDto::id))
    }

    override suspend fun findMessage(conversationId: String, messageId: String): SupabaseMessageDto? {
        return messageRows.find { it.conversationId == conversationId && it.id == messageId }
    }

    override suspend fun findLatestMessage(conversationId: String): SupabaseMessageDto? {
        return getMessages(conversationId).lastOrNull()
    }

    override suspend fun insertMessage(message: SupabaseMessageInsertDto) {
        insertedMessage = message
        val senderId = checkNotNull(authenticatedUserId)
        val participant = participantRows.first { row ->
            row.conversationId == message.conversationId && row.userId == senderId
        }
        messageRows += SupabaseMessageDto(
            id = message.id,
            conversationId = message.conversationId,
            senderId = senderId,
            content = message.content,
            createdAt = message.createdAt,
            senderProfile = participant.profile
        )
        conversationRows[message.conversationId] = SupabaseConversationDto(message.conversationId, message.createdAt)
    }

    override suspend fun insertReport(report: SupabaseMessageReportInsertDto) {
        insertedReport = report
        val index = messageRows.indexOfFirst { it.id == report.messageId }
        check(index >= 0)
        messageRows[index] = messageRows[index].copy(
            reports = messageRows[index].reports + SupabaseMessageReportMarkerDto("report-${report.messageId}")
        )
    }

    fun replaceMessageTimestamp(timestamp: String) {
        check(messageRows.isNotEmpty())
        messageRows[0] = messageRows[0].copy(createdAt = timestamp)
    }

    private fun toMessageDto(message: Message): SupabaseMessageDto {
        return SupabaseMessageDto(
            id = message.id.value,
            conversationId = message.conversationId.value,
            senderId = message.senderId.value,
            content = message.content,
            createdAt = Instant.fromEpochMilliseconds(message.timestampMillis).toString(),
            senderProfile = SupabaseMessageProfileDto(message.senderName),
            reports = if (message.isReported) listOf(SupabaseMessageReportMarkerDto("existing-report")) else emptyList()
        )
    }
}
