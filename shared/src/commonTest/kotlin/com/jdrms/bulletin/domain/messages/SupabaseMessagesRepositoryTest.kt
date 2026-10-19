package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseConversationInboxDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageInsertDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageReportInsertDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageReportType
import com.jdrms.bulletin.domain.messages.infrastructure.repository.MessagesPersistenceException
import com.jdrms.bulletin.domain.messages.infrastructure.repository.PostgrestSupabaseMessagesTable
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesTable
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Instant

class SupabaseMessagesRepositoryContractTest : MessagesRepositoryContractTest() {
    override fun createRepository(conversations: List<Conversation>, messages: List<Message>): MessagesRepository {
        return SupabaseMessagesRepository(FakeSupabaseMessagesTable(conversations, messages))
    }
}

class SupabaseMessagesRepositoryTest {
    @Test
    fun inboxRequestExcludesDeletedMemberships() = runTest {
        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Postgrest)
            httpEngine = MockEngine { request ->
                assertEquals("eq.alice-id", request.url.parameters["membership.user_id"])
                assertEquals("is.null", request.url.parameters["membership.deleted_at"])
                respond("[]", HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
            }
        }

        try {
            assertEquals(emptyList(), PostgrestSupabaseMessagesTable(client).getInboxConversations(alice.id.value))
        } finally {
            client.close()
        }
    }

    @Test
    fun incomingMessageResurfacesConversationForParticipantWhoHidIt() = runTest {
        val table = FakeSupabaseMessagesTable(listOf(conversation()))
        val repository = SupabaseMessagesRepository(table)

        repository.hideConversation(alice.id, conversationId).getOrThrow()
        assertEquals(emptyList(), repository.getConversations(alice.id).getOrThrow())

        table.authenticatedUserId = carol.id.value
        repository.sendMessage(message(carol)).getOrThrow()

        assertEquals(listOf(conversationId), repository.getConversations(alice.id).getOrThrow().map { it.id })
    }

    @Test
    fun conversationVisibilityRpcsDecodeScalarBooleanResponses() = runTest {
        val requestedPaths = mutableListOf<String>()
        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Postgrest)
            httpEngine = MockEngine { request ->
                requestedPaths += request.url.encodedPath
                val body = if (request.url.encodedPath.endsWith("hide_conversation")) "true" else "false"
                respond(body, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
            }
        }

        try {
            val table = PostgrestSupabaseMessagesTable(client)
            assertTrue(table.hideConversation(conversationId.value))
            assertFalse(table.restoreConversation(conversationId.value))
            assertEquals(
                listOf("/rest/v1/rpc/hide_conversation", "/rest/v1/rpc/restore_conversation"),
                requestedPaths
            )
        } finally {
            client.close()
        }
    }

    @Test
    fun sendWritesTransportFieldsWithoutClientControlledIdentity() = runTest {
        val table = FakeSupabaseMessagesTable(listOf(conversation()))
        val repository = SupabaseMessagesRepository(table)

        assertEquals(Result.Success(message()), repository.sendMessage(message()))

        assertEquals(
            SupabaseMessageInsertDto(
                id = "message-a",
                conversationId = "conversation-a",
                content = "Hello"
            ),
            table.insertedMessage
        )
    }

    @Test
    fun sendReturnsServerTimestampInsteadOfClientTimestamp() = runTest {
        val table = FakeSupabaseMessagesTable(listOf(conversation()))
        val clientMessage = message().copy(timestampMillis = 1L)

        val sent = SupabaseMessagesRepository(table).sendMessage(clientMessage).getOrThrow()

        assertEquals(42L, sent.timestampMillis)
        assertEquals("1970-01-01T00:00:00.042Z", table.findMessage(conversationId.value, sent.id.value)?.createdAt)
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
    fun hideRejectsClaimedIdentityThatDoesNotMatchSession() = runTest {
        val table = FakeSupabaseMessagesTable(listOf(conversation()))

        val repository = SupabaseMessagesRepository(table)
        val result = repository.hideConversation(carol.id, conversationId)

        assertIs<ConversationAccessException>(assertIs<Result.Error>(result).exception)
        assertEquals(listOf(conversationId), repository.getConversations(alice.id).getOrThrow().map { it.id })
    }

    @Test
    fun hideReturnsPersistenceFailureWithoutChangingInboxWhenStorageFails() = runTest {
        val table = object : SupabaseMessagesTable by FakeSupabaseMessagesTable(listOf(conversation())) {
            override suspend fun hideConversation(conversationId: String): Boolean {
                error("storage unavailable")
            }
        }

        val result = SupabaseMessagesRepository(table).hideConversation(alice.id, conversationId)

        assertIs<MessagesPersistenceException>(assertIs<Result.Error>(result).exception)
    }

    @Test
    fun startingSameListingConversationAgainRestoresRequestersInbox() = runTest {
        val existing = conversation()
        val table = FakeSupabaseMessagesTable(
            listOf(
                Conversation(
                    existing.id,
                    existing.participants,
                    existing.lastMessage,
                    existing.updatedAtMillis,
                    ListingReferenceId("listing-a")
                )
            )
        )
        val repository = SupabaseMessagesRepository(table)
        repository.hideConversation(alice.id, conversationId).getOrThrow()

        val reopened = repository.getOrCreateConversation(
            alice.id,
            ConversationParticipant(carol.id, carol.displayName),
            ListingReferenceId("listing-a")
        )

        assertEquals(conversationId, reopened.getOrThrow().id)
        assertEquals(listOf(conversationId), repository.getConversations(alice.id).getOrThrow().map { it.id })
    }

    @Test
    fun inboxUsesEachViewersHistoryCutoffAfterReopening() = runTest {
        val listingId = ListingReferenceId("listing-a")
        val existing = conversation()
        val table = FakeSupabaseMessagesTable(
            listOf(Conversation(existing.id, existing.participants, listingId = listingId)),
            listOf(message())
        )
        val repository = SupabaseMessagesRepository(table)

        repository.hideConversation(alice.id, conversationId).getOrThrow()
        repository.getOrCreateConversation(
            alice.id,
            ConversationParticipant(carol.id, carol.displayName),
            listingId
        ).getOrThrow()
        table.authenticatedUserId = carol.id.value
        val betweenCutoffs = repository.sendMessage(message(carol, id = MessageId("between-cutoffs"))).getOrThrow()
        repository.hideConversation(carol.id, conversationId).getOrThrow()
        repository.getOrCreateConversation(
            carol.id,
            ConversationParticipant(alice.id, alice.displayName),
            listingId
        ).getOrThrow()

        table.authenticatedUserId = alice.id.value
        val aliceInbox = repository.getConversations(alice.id).getOrThrow().single()
        table.authenticatedUserId = carol.id.value
        val carolInbox = repository.getConversations(carol.id).getOrThrow().single()

        assertEquals(Instant.fromEpochMilliseconds(43), aliceInbox.historyVisibleAfter)
        assertEquals(Instant.fromEpochMilliseconds(45), carolInbox.historyVisibleAfter)
        assertEquals(betweenCutoffs, aliceInbox.lastMessage)
        assertNull(carolInbox.lastMessage)
    }

    @Test
    fun startingAlreadyVisibleListingConversationSucceeds() = runTest {
        val existing = conversation()
        val table = FakeSupabaseMessagesTable(
            listOf(
                Conversation(
                    existing.id,
                    existing.participants,
                    existing.lastMessage,
                    existing.updatedAtMillis,
                    ListingReferenceId("listing-a")
                )
            )
        )

        val result = SupabaseMessagesRepository(table).getOrCreateConversation(
            alice.id,
            ConversationParticipant(carol.id, carol.displayName),
            ListingReferenceId("listing-a")
        )

        assertEquals(conversationId, result.getOrThrow().id)
    }

    @Test
    fun reportWritesAuthenticatedReporterWithPlaceholderReason() = runTest {
        val table = FakeSupabaseMessagesTable(
            conversations = listOf(conversation()),
            messages = listOf(message()),
            authenticatedUserId = carol.id.value
        )
        val repository = SupabaseMessagesRepository(table)

        repository.reportMessage(carol.id, conversationId, message().id, "Harassment").getOrThrow()

        assertEquals(
            SupabaseMessageReportInsertDto(
                "message-a",
                "carol-id",
                "Inappropriate Content",
                SupabaseMessageReportType.MESSAGE,
                null
            ),
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
            override suspend fun getInboxConversations(userId: String) = throw cause
        }

        val result = assertIs<Result.Error>(SupabaseMessagesRepository(table).getConversations(alice.id))

        assertEquals("Unable to access messages. Please try again.", result.message)
        assertIs<MessagesPersistenceException>(result.exception)
        assertSame(cause, result.exception.cause)
    }

    @Test
    fun cancellationFromStorageIsRethrown() = runTest {
        val table = object : SupabaseMessagesTable by FakeSupabaseMessagesTable(emptyList()) {
            override suspend fun getInboxConversations(userId: String): List<SupabaseConversationInboxDto> {
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

        val result = SupabaseMessagesRepository(table).getMessagePage(alice.id, conversationId)

        assertIs<MessagesPersistenceException>(assertIs<Result.Error>(result).exception)
    }
}
