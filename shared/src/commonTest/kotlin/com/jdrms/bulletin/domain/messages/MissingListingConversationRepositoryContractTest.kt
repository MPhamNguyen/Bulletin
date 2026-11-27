package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationLockReason
import com.jdrms.bulletin.domain.messages.domain.model.ConversationReadOnlyException
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageEdit
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

abstract class MissingListingConversationRepositoryContractTest {
    protected abstract fun repository(
        conversation: Conversation,
        messages: List<Message>,
        viewer: SenderId
    ): MessagesRepository

    @Test
    fun missingListingLocksWritesWhilePreservingHistoryForBothParticipants() = runTest {
        val messages = listOf(message(), message(carol, id = MessageId("reply")))
        val thread = Conversation(conversationId, conversation().participants, lastMessage = messages.last())
        for (participant in thread.participants) {
            val repository = repository(thread, messages, participant.id)
            val loaded = repository.getConversations(participant.id).getOrThrow().single()
            assertEquals(ConversationLockReason.LISTING_UNAVAILABLE, loaded.lockReason)
            assertEquals(messages, repository.getMessages(participant.id, conversationId).getOrThrow())
            val own = messages.single { it.senderId == participant.id }
            val sent = repository.sendMessage(own.copy(id = MessageId("new")))
            assertEquals(
                ConversationLockReason.LISTING_UNAVAILABLE,
                assertIs<ConversationReadOnlyException>(assertIs<Result.Error>(sent).exception).reason
            )
            val edited = repository.editMessage(participant.id, MessageEdit(conversationId, own.id, "Changed", null))
            assertEquals(
                ConversationLockReason.LISTING_UNAVAILABLE,
                assertIs<ConversationReadOnlyException>(assertIs<Result.Error>(edited).exception).reason
            )
            assertEquals(messages, repository.getMessages(participant.id, conversationId).getOrThrow())
        }
    }
}

class InMemoryMissingListingConversationTest : MissingListingConversationRepositoryContractTest() {
    override fun repository(
        conversation: Conversation,
        messages: List<Message>,
        viewer: SenderId
    ): MessagesRepository =
        messagesRepository(listOf(conversation), messages)
}

class SupabaseMissingListingConversationTest : MissingListingConversationRepositoryContractTest() {
    override fun repository(
        conversation: Conversation,
        messages: List<Message>,
        viewer: SenderId
    ): MessagesRepository = SupabaseMessagesRepository(
        FakeSupabaseMessagesTable(listOf(conversation), messages, authenticatedUserId = viewer.value)
    )
}
