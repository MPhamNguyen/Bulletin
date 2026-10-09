package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.CurrentMessageSenderProvider
import com.jdrms.bulletin.domain.messages.application.GetConversationMessages
import com.jdrms.bulletin.domain.messages.application.GetConversations
import com.jdrms.bulletin.domain.messages.application.HasListingConversation
import com.jdrms.bulletin.domain.messages.application.MessageSeller
import com.jdrms.bulletin.domain.messages.application.MessageSenderLookupException
import com.jdrms.bulletin.domain.messages.application.MessagingAuthenticationRequiredException
import com.jdrms.bulletin.domain.messages.application.ReportMessage
import com.jdrms.bulletin.domain.messages.application.SendMessage
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MessagesUseCasesTest {
    private val provider = MutableMessageSenderProvider()
    private val repository = RecordingMessagesRepository()

    @Test
    fun sendBuildsMessageFromCurrentIdentityWithFullGeneratedId() = runTest {
        val uuid = MessageId("01374c6d-8e85-4e08-b8e1-40f65faf3184")
        val send = SendMessage(repository, provider, nextMessageId = { uuid }, nowMillis = { 123L })
        val sent = send(conversationId, "  Hello  ").getOrThrow()
        assertEquals(Message(uuid, conversationId, alice.id, alice.displayName, "Hello", 123L), sent)
        provider.result = Result.Success(bob)
        val next = send(conversationId, "Hello").getOrThrow()
        assertEquals(bob.id, next.senderId)
        assertEquals(bob.displayName, next.senderName)
    }

    @Test
    fun messageSellerCreatesListingScopedConversationAndSendsOnce() = runTest {
        val messageSeller = MessageSeller(
            repository,
            provider,
            nextMessageId = { MessageId("seller-message") },
            nowMillis = { 123L }
        )
        val result = messageSeller(
            ListingReferenceId("listing-42"),
            ConversationParticipant(bob.id, bob.displayName),
            " Hi, is this available? "
        )
        assertEquals("listing-42", result.getOrThrow().listingId?.value)
        assertEquals(1, repository.createdConversationCount)
        assertEquals(1, repository.sentMessageCount)
    }

    @Test
    fun messageSellerRejectsOwnListingBeforePersistence() = runTest {
        val result = MessageSeller(repository, provider)(
            ListingReferenceId("listing-42"),
            ConversationParticipant(alice.id, alice.displayName),
            "Hello"
        )
        assertTrue(result.isError())
        assertEquals(0, repository.createdConversationCount)
        assertEquals(0, repository.sentMessageCount)
    }

    @Test
    fun hasListingConversationMatchesTheExactListingAndSeller() = runTest {
        repository.conversations = listOf(
            Conversation(
                ConversationId("listing-one-chat"),
                listOf(
                    ConversationParticipant(alice.id, alice.displayName),
                    ConversationParticipant(bob.id, bob.displayName)
                ),
                listingId = ListingReferenceId("listing-one")
            )
        )
        val hasConversation = HasListingConversation(repository, provider)

        assertEquals(true, hasConversation(ListingReferenceId("listing-one"), bob.id).getOrThrow())
        assertEquals(false, hasConversation(ListingReferenceId("listing-two"), bob.id).getOrThrow())
    }

    @Test
    fun readsAndReportsResolveCurrentIdentityOnEveryCall() = runTest {
        val conversations = GetConversations(repository, provider)
        val messages = GetConversationMessages(repository, provider)
        val report = ReportMessage(repository, provider)
        listOf(alice, bob).forEach { sender ->
            provider.result = Result.Success(sender)
            conversations().getOrThrow()
            messages(conversationId).getOrThrow()
            report(conversationId, message().id, "Spam").getOrThrow()
            assertEquals(List(3) { sender.id }, repository.callers.takeLast(3))
        }
    }

    @Test
    fun missingSessionAndIdentityFailurePreventAllRepositoryCalls() = runTest {
        listOf(
            MessagingAuthenticationRequiredException(),
            MessageSenderLookupException(IllegalStateException("Auth unavailable"))
        ).forEach { error ->
            val failure = Result.Error(error)
            provider.result = failure
            assertEquals(failure, GetConversations(repository, provider)())
            assertEquals(failure, GetConversationMessages(repository, provider)(conversationId))
            assertEquals(failure, SendMessage(repository, provider)(conversationId, "Hello"))
            assertEquals(failure, ReportMessage(repository, provider)(conversationId, message().id, "Spam"))
        }
        assertTrue(repository.callers.isEmpty())
    }

    @Test
    fun messageValidationRunsBeforePersistenceAndAcceptsLengthBoundary() = runTest {
        val send = SendMessage(repository, provider, nextMessageId = { MessageId("fixed") }, nowMillis = { 0L })
        assertTrue(send(conversationId, "  ").isError())
        assertTrue(send(conversationId, "a".repeat(1001)).isError())
        assertTrue(repository.callers.isEmpty())
        assertTrue(send(conversationId, "a".repeat(1000)).isSuccess())
    }

    @Test
    fun blankReportReasonDoesNotCallRepository() = runTest {
        assertTrue(ReportMessage(repository, provider)(conversationId, message().id, "  ").isError())
        assertTrue(repository.callers.isEmpty())
    }

    @Test
    fun repositoryFailuresAreReturnedByEveryUseCase() = runTest {
        val failure = Result.Error(IllegalStateException("Unavailable"))
        repository.failure = failure
        assertEquals(failure, GetConversations(repository, provider)())
        assertEquals(failure, GetConversationMessages(repository, provider)(conversationId))
        assertEquals(failure, SendMessage(repository, provider)(conversationId, "Hello"))
        assertEquals(failure, ReportMessage(repository, provider)(conversationId, message().id, "Spam"))
    }

    @Test
    fun authCancellationPropagatesWithoutCallingRepository() = runTest {
        val cancelled = CurrentMessageSenderProvider { throw CancellationException("Cancelled") }
        assertFailsWith<CancellationException> { GetConversations(repository, cancelled)() }
        assertFailsWith<CancellationException> { GetConversationMessages(repository, cancelled)(conversationId) }
        assertFailsWith<CancellationException> { SendMessage(repository, cancelled)(conversationId, "Hello") }
        assertFailsWith<CancellationException> {
            ReportMessage(repository, cancelled)(conversationId, message().id, "Spam")
        }
        assertTrue(repository.callers.isEmpty())
    }

    private class RecordingMessagesRepository : MessagesRepository {
        val callers = mutableListOf<SenderId>()
        var createdConversationCount = 0
        var sentMessageCount = 0
        var conversations: List<Conversation> = emptyList()
        var failure: Result.Error? = null

        override suspend fun getOrCreateConversation(
            requesterId: SenderId,
            otherParticipant: ConversationParticipant,
            listingId: ListingReferenceId
        ): Result<Conversation> {
            createdConversationCount++
            return failure ?: Result.Success(
                Conversation(
                    conversationId,
                    listOf(
                        ConversationParticipant(requesterId, "Requester"), otherParticipant
                    ),
                    listingId = listingId
                )
            )
        }

        override suspend fun getConversations(userId: SenderId): Result<List<Conversation>> {
            callers.add(userId)
            return failure ?: Result.Success(conversations.filter { it.includes(userId) })
        }

        override suspend fun getMessages(userId: SenderId, conversationId: ConversationId): Result<List<Message>> {
            callers.add(userId)
            return failure ?: Result.Success(emptyList())
        }

        override suspend fun sendMessage(message: Message): Result<Message> {
            sentMessageCount++
            callers.add(message.senderId)
            return failure ?: Result.Success(message)
        }

        override suspend fun reportMessage(
            userId: SenderId,
            conversationId: ConversationId,
            messageId: MessageId,
            reason: String
        ): Result<Unit> {
            callers.add(userId)
            return failure ?: Result.Success(Unit)
        }
    }
}
