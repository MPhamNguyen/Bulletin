package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** Reuse this suite for BULLETIN-85 with a deterministic fake of the Supabase transport. */
abstract class MessagesRepositoryContractTest {
    protected abstract fun createRepository(
        conversations: List<Conversation>,
        messages: List<Message> = emptyList()
    ): MessagesRepository

    @Test
    fun conversationListingUsesIdsEvenWhenDisplayNamesMatch() = runTest {
        val other = conversation(ConversationId("conversation-b"), listOf(bob, carol))
        val repository = createRepository(listOf(conversation(), other))
        assertEquals(listOf(conversationId), repository.getConversations(alice.id).getOrThrow().map { it.id })
        assertEquals(listOf(other.id), repository.getConversations(bob.id).getOrThrow().map { it.id })
        assertEquals(2, repository.getConversations(carol.id).getOrThrow().size)
    }

    @Test
    fun emptyRepositoryDoesNotExposeDemoConversations() = runTest {
        val repository = createRepository(emptyList())
        assertEquals(emptyList(), repository.getConversations(alice.id).getOrThrow())
    }

    @Test
    fun readsRejectOutsidersAndUnknownConversationsWithoutLeakingExistence() = runTest {
        val repository = createRepository(listOf(conversation()), listOf(message()))
        assertAccessDenied(repository.getMessages(bob.id, conversationId))
        assertAccessDenied(repository.getMessages(alice.id, ConversationId("missing")))
        assertEquals(listOf(message()), repository.getMessages(alice.id, conversationId).getOrThrow())
    }

    @Test
    fun participantSendPreservesIdentityAndUpdatesSummary() = runTest {
        val repository = createRepository(listOf(conversation()))
        val sent = message()
        assertEquals(Result.Success(sent), repository.sendMessage(sent))
        assertEquals(listOf(sent), repository.getMessages(carol.id, conversationId).getOrThrow())
        val updated = repository.getConversations(alice.id).getOrThrow().single()
        assertEquals(sent, updated.lastMessage)
        assertEquals(sent.timestampMillis, updated.updatedAtMillis)
    }

    @Test
    fun rejectedSendDoesNotCreateConversationsOrMessages() = runTest {
        val repository = createRepository(listOf(conversation()))
        assertAccessDenied(repository.sendMessage(message(bob)))
        assertAccessDenied(repository.sendMessage(message(inConversation = ConversationId("missing"))))
        assertEquals(emptyList(), repository.getMessages(alice.id, conversationId).getOrThrow())
        assertEquals(1, repository.getConversations(alice.id).getOrThrow().size)
        assertEquals(null, repository.getConversations(alice.id).getOrThrow().single().lastMessage)
    }

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
        assertTrue(repository.reportMessage(carol.id, conversationId, message().id, "Spam").isSuccess())
        assertTrue(repository.getMessages(alice.id, conversationId).getOrThrow().single().isReported)
        assertEquals(message().report(), repository.getConversations(alice.id).getOrThrow().single().lastMessage)
    }

    @Test
    fun reportingOlderMessageDoesNotReplaceLatestSummary() = runTest {
        val repository = createRepository(listOf(conversation()), listOf(message()))
        val latest = message(carol, id = MessageId("latest"))
        repository.sendMessage(latest).getOrThrow()
        repository.reportMessage(alice.id, conversationId, message().id, "Spam").getOrThrow()
        assertEquals(latest, repository.getConversations(alice.id).getOrThrow().single().lastMessage)
    }

    @Test
    fun returnedMessageListsRemainSnapshotsAfterSending() = runTest {
        val repository = createRepository(listOf(conversation()))
        val before = repository.getMessages(alice.id, conversationId).getOrThrow()
        repository.sendMessage(message()).getOrThrow()
        assertEquals(emptyList(), before)
    }

    private fun assertAccessDenied(result: Result<*>) {
        assertIs<ConversationAccessException>(assertIs<Result.Error>(result).exception)
    }
}

class InMemoryMessagesRepositoryTest : MessagesRepositoryContractTest() {
    override fun createRepository(conversations: List<Conversation>, messages: List<Message>): MessagesRepository =
        messagesRepository(conversations, messages)
}
