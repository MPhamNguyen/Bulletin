package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationReadOnlyException
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageEdit
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

abstract class RetainedConversationRepositoryContractTest {
    private val retained = message(carol).copy(senderId = null)
    private val own = message(id = MessageId("last-own")).copy(timestampMillis = 43L)
    private val thread = conversation(senders = listOf(alice)).recordMessage(own).getOrThrow()
    protected abstract fun repository(conversation: Conversation, messages: List<Message>): MessagesRepository

    @Test
    fun remainingParticipantCanReadAndPageAllRetainedMessages() = runTest {
        val repository = repository(thread, listOf(retained, own))
        assertEquals(conversationId, repository.getConversations(alice.id).getOrThrow().single().id)
        val first = repository.getMessagePage(alice.id, conversationId, limit = 1).getOrThrow()
        assertEquals(own.id, first.messages.single().id)
        val older = repository.getMessagePage(alice.id, conversationId, before = first.olderCursor).getOrThrow()
        assertEquals(retained.content, older.messages.single().content)
        assertNull(older.messages.single().senderId)
        assertEquals("User Not Found", older.messages.single().senderName)
        assertIs<Result.Error>(repository.getMessagePage(carol.id, conversationId))
        assertEquals(emptyList(), repository.getConversations(carol.id).getOrThrow())
    }

    @Test
    fun sendsAndEditsAreRejectedWhileHistoryAndReportingRemainAvailable() = runTest {
        val repository = repository(thread, listOf(retained, own))
        val sent = repository.sendMessage(message(id = MessageId("new")))
        assertIs<ConversationReadOnlyException>(assertIs<Result.Error>(sent).exception)
        assertIs<Result.Error>(repository.sendMessage(retained))
        val edit = repository.editMessage(alice.id, MessageEdit(conversationId, own.id, "Changed", null))
        assertIs<ConversationReadOnlyException>(assertIs<Result.Error>(edit).exception)
        repository.reportMessage(alice.id, conversationId, retained.id, "Spam").getOrThrow()
        val history = repository.getMessages(alice.id, conversationId).getOrThrow()
        assertEquals(2, history.size)
        assertEquals(own.content, history.last().content)
    }
}

class InMemoryRetainedConversationTest : RetainedConversationRepositoryContractTest() {
    override fun repository(conversation: Conversation, messages: List<Message>): MessagesRepository =
        messagesRepository(listOf(conversation), messages)
}

class SupabaseRetainedConversationTest : RetainedConversationRepositoryContractTest() {
    override fun repository(conversation: Conversation, messages: List<Message>): MessagesRepository =
        SupabaseMessagesRepository(FakeSupabaseMessagesTable(listOf(conversation), messages))
}
