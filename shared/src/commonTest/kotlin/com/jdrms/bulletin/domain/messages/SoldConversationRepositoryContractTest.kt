package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationLockReason
import com.jdrms.bulletin.domain.messages.domain.model.ConversationReadOnlyException
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageEdit
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

abstract class SoldConversationRepositoryContractTest {
    protected abstract fun repository(conversation: Conversation, messages: List<Message>): MessagesRepository

    @Test
    fun soldListingPreservesHistoryAndRejectsSendsAndEditsForBothParticipants() = runTest {
        val own = message()
        val thread = Conversation(
            id = conversationId,
            participants = conversation().participants,
            listingId = ListingReferenceId("sold-listing"),
            lastMessage = own,
            isListingSold = true
        )
        val repository = repository(thread, listOf(own))
        val loaded = repository.getConversations(alice.id).getOrThrow().single()
        assertTrue(loaded.isReadOnly)
        assertEquals(ConversationLockReason.LISTING_SOLD, loaded.lockReason)
        assertEquals(listOf(own), repository.getMessages(alice.id, conversationId).getOrThrow())

        val send = assertIs<Result.Error>(repository.sendMessage(message(id = MessageId("new"))))
        assertEquals(
            ConversationLockReason.LISTING_SOLD,
            assertIs<ConversationReadOnlyException>(send.exception).reason
        )
        val edit = assertIs<Result.Error>(
            repository.editMessage(alice.id, MessageEdit(conversationId, own.id, "Changed", null))
        )
        assertEquals(
            ConversationLockReason.LISTING_SOLD,
            assertIs<ConversationReadOnlyException>(edit.exception).reason
        )
        for (participant in thread.participants) {
            val denied = assertIs<Result.Error>(loaded.authorizeSending(participant.id))
            assertIs<ConversationReadOnlyException>(denied.exception)
        }
        repository.markConversationRead(alice.id, conversationId).getOrThrow()
        assertTrue(repository.getConversations(alice.id).getOrThrow().single().isReadOnly)
        repository.reportMessage(alice.id, conversationId, own.id, "Spam").getOrThrow()
        assertEquals(own.content, repository.getMessages(alice.id, conversationId).getOrThrow().single().content)
    }
}

class InMemorySoldConversationTest : SoldConversationRepositoryContractTest() {
    override fun repository(conversation: Conversation, messages: List<Message>): MessagesRepository =
        messagesRepository(listOf(conversation), messages)
}

class SupabaseSoldConversationTest : SoldConversationRepositoryContractTest() {
    override fun repository(conversation: Conversation, messages: List<Message>): MessagesRepository =
        SupabaseMessagesRepository(FakeSupabaseMessagesTable(listOf(conversation), messages))
}
