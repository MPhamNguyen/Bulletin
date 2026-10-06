package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.CurrentMessageSenderProvider
import com.jdrms.bulletin.domain.messages.application.GetConversationMessages
import com.jdrms.bulletin.domain.messages.application.GetConversations
import com.jdrms.bulletin.domain.messages.application.MessageSenderLookupException
import com.jdrms.bulletin.domain.messages.application.MessagingAuthenticationRequiredException
import com.jdrms.bulletin.domain.messages.application.ReportMessage
import com.jdrms.bulletin.domain.messages.application.SendMessage
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
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
        var failure: Result.Error? = null

        override suspend fun getConversations(userId: SenderId): Result<List<Conversation>> {
            callers.add(userId)
            return failure ?: Result.Success(emptyList())
        }

        override suspend fun getMessages(userId: SenderId, conversationId: ConversationId): Result<List<Message>> {
            callers.add(userId)
            return failure ?: Result.Success(emptyList())
        }

        override suspend fun sendMessage(message: Message): Result<Message> {
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
