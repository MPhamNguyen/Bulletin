package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.MessageReportAlreadySubmittedException
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

abstract class MessageReportingRepositoryContractTest {
    protected abstract fun createRepository(
        conversations: List<Conversation>,
        messages: List<Message> = emptyList()
    ): MessagesRepository

    @Test
    fun reportsRequireMembershipAndTheMessageMustBelongToTheConversation() = runTest {
        val other = conversation(ConversationId("conversation-b"))
        val repository = createRepository(listOf(conversation(), other), listOf(message()))
        assertAccessDenied(repository.reportMessage(bob.id, conversationId, message().id, "Spam"))
        assertAccessDenied(repository.reportMessage(alice.id, other.id, message().id, "Spam"))
        assertAccessDenied(repository.reportMessage(alice.id, ConversationId("missing"), message().id, "Spam"))
        assertAccessDenied(repository.reportMessage(alice.id, conversationId, MessageId("missing"), "Spam"))
        assertFalse(repository.getMessages(alice.id, conversationId).getOrThrow().single().isReported)
    }

    @Test
    fun reportUpdatesMessageAndMatchingConversationSummary() = runTest {
        val repository = createRepository(listOf(conversation()))
        repository.sendMessage(message()).getOrThrow()
        assertTrue(repository.reportMessage(alice.id, conversationId, message().id, "Spam").isSuccess())
        assertTrue(repository.getMessages(alice.id, conversationId).getOrThrow().single().isReported)
        assertEquals(message().report(), repository.getConversations(alice.id).getOrThrow().single().lastMessage)
    }

    @Test
    fun reportingOlderMessageDoesNotReplaceLatestSummary() = runTest {
        val repository = createRepository(listOf(conversation()), listOf(message()))
        val latest = message(alice, id = MessageId("latest")).copy(timestampMillis = 43L)
        repository.sendMessage(latest).getOrThrow()
        repository.reportMessage(alice.id, conversationId, message().id, "Spam").getOrThrow()
        assertEquals(latest, repository.getConversations(alice.id).getOrThrow().single().lastMessage)
    }

    @Test
    fun sameReporterCannotSubmitAnotherOpenReportForTheSameMessage() = runTest {
        val repository = createRepository(listOf(conversation()), listOf(message()))
        repository.reportMessage(alice.id, conversationId, message().id, "Spam").getOrThrow()

        val result = repository.reportMessage(alice.id, conversationId, message().id, "Harassment")

        assertIs<MessageReportAlreadySubmittedException>(assertIs<Result.Error>(result).exception)
        assertTrue(repository.getMessages(alice.id, conversationId).getOrThrow().single().isReported)
    }

    @Test
    fun sameReporterCanReportDifferentMessages() = runTest {
        val second = message(id = MessageId("second"))
        val repository = createRepository(listOf(conversation()), listOf(message(), second))

        repository.reportMessage(alice.id, conversationId, message().id, "Spam").getOrThrow()
        repository.reportMessage(alice.id, conversationId, second.id, "Spam").getOrThrow()

        assertTrue(repository.getMessages(alice.id, conversationId).getOrThrow().all { it.isReported })
    }

    private fun assertAccessDenied(result: Result<*>) {
        assertIs<ConversationAccessException>(assertIs<Result.Error>(result).exception)
    }
}

class InMemoryMessageReportingRepositoryTest : MessageReportingRepositoryContractTest() {
    override fun createRepository(conversations: List<Conversation>, messages: List<Message>): MessagesRepository =
        messagesRepository(conversations, messages)
}

class SupabaseMessageReportingRepositoryTest : MessageReportingRepositoryContractTest() {
    override fun createRepository(conversations: List<Conversation>, messages: List<Message>): MessagesRepository =
        SupabaseMessagesRepository(FakeSupabaseMessagesTable(conversations, messages))
}
