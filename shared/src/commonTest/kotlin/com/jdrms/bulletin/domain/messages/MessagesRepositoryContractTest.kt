package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.ConversationParticipant
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

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
    fun hidingConversationAffectsOnlyRequestingParticipantsInbox() = runTest {
        val repository = createRepository(listOf(conversation()), listOf(message()))

        assertEquals(Result.Success(Unit), repository.hideConversation(alice.id, conversationId))
        assertEquals(Result.Success(Unit), repository.hideConversation(alice.id, conversationId))
        assertEquals(emptyList(), repository.getConversations(alice.id).getOrThrow())
        assertEquals(listOf(conversationId), repository.getConversations(carol.id).getOrThrow().map { it.id })
        assertEquals(emptyList(), repository.getMessages(alice.id, conversationId).getOrThrow())
        assertEquals(listOf(message()), repository.getMessages(carol.id, conversationId).getOrThrow())
    }

    @Test
    fun newMessageRestoresConversationForParticipantsWhoPreviouslyHidIt() = runTest {
        val repository = createRepository(listOf(conversation()))

        repository.hideConversation(alice.id, conversationId).getOrThrow()
        assertEquals(emptyList(), repository.getConversations(alice.id).getOrThrow())

        repository.sendMessage(message()).getOrThrow()

        assertEquals(listOf(conversationId), repository.getConversations(alice.id).getOrThrow().map { it.id })
    }

    @Test
    fun restoredConversationShowsOnlyMessagesAfterItsLastRemovalForThatParticipant() = runTest {
        val old = message(id = MessageId("old")).copy(timestampMillis = 41L)
        val boundary = message(id = MessageId("boundary")).copy(timestampMillis = 42L)
        val recent = message(id = MessageId("recent")).copy(timestampMillis = 43L)
        val listingId = ListingReferenceId("listing-a")
        val repository = createRepository(listOf(conversation().withListing(listingId)), listOf(old, boundary))

        repository.hideConversation(alice.id, conversationId).getOrThrow()
        assertEquals(emptyList(), repository.getMessages(alice.id, conversationId).getOrThrow())
        repository.getOrCreateConversation(alice.id, ConversationParticipant(carol.id, carol.displayName), listingId)
            .getOrThrow()
        assertEquals(emptyList(), repository.getMessages(alice.id, conversationId).getOrThrow())
        val sent = repository.sendMessage(recent).getOrThrow()
        assertEquals(listOf(sent), repository.getMessages(alice.id, conversationId).getOrThrow())
        assertEquals(listOf(old, boundary, sent), repository.getMessages(carol.id, conversationId).getOrThrow())
        assertEquals(recent.id, repository.getConversations(alice.id).getOrThrow().single().lastMessage?.id)
        assertEquals(null, repository.getMessagePage(alice.id, conversationId, limit = 1).getOrThrow().olderCursor)
    }

    @Test
    fun hidingUnknownConversationOrAsOutsiderIsDenied() = runTest {
        val repository = createRepository(listOf(conversation()))

        assertAccessDenied(repository.hideConversation(bob.id, conversationId))
        assertAccessDenied(repository.hideConversation(alice.id, ConversationId("missing")))
        assertEquals(listOf(conversationId), repository.getConversations(alice.id).getOrThrow().map { it.id })
    }

    @Test
    fun conversationCreationIsListingScopedAndIdempotent() = runTest {
        val repository = createRepository(emptyList())
        val seller = ConversationParticipant(carol.id, carol.displayName)
        val firstListing = ListingReferenceId("listing-one")
        val secondListing = ListingReferenceId("listing-two")

        val first = repository.getOrCreateConversation(alice.id, seller, firstListing).getOrThrow()
        val repeated = repository.getOrCreateConversation(alice.id, seller, firstListing).getOrThrow()
        val second = repository.getOrCreateConversation(alice.id, seller, secondListing).getOrThrow()

        assertEquals(first.id, repeated.id)
        assertEquals(firstListing, repeated.listingId)
        assertTrue(first.id != second.id)
        assertEquals(secondListing, second.listingId)
        assertEquals(2, repository.getConversations(alice.id).getOrThrow().size)
        assertEquals(2, repository.getConversations(carol.id).getOrThrow().size)
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
    fun sendingMessageKeepsListingReferenceInInboxSummary() = runTest {
        val listingId = ListingReferenceId("listing-a")
        val repository = createRepository(listOf(conversation().withListing(listingId)))

        repository.sendMessage(message()).getOrThrow()

        assertEquals(listingId, repository.getConversations(alice.id).getOrThrow().single().listingId)
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
    fun returnedMessageListsRemainSnapshotsAfterSending() = runTest {
        val repository = createRepository(listOf(conversation()))
        val before = repository.getMessages(alice.id, conversationId).getOrThrow()
        repository.sendMessage(message()).getOrThrow()
        assertEquals(emptyList(), before)
    }

    @Test
    fun pagesUseTimestampAndIdWithoutRepeatingTiedMessages() = runTest {
        val messages = (1..5).map { index ->
            message(id = MessageId("message-$index")).copy(timestampMillis = 42L)
        }
        val repository = createRepository(listOf(conversation()), messages.reversed())
        val newest = repository.getMessagePage(alice.id, conversationId, limit = 2).getOrThrow()
        assertEquals(listOf("message-4", "message-5"), newest.messages.map { it.id.value })
        val middle = repository.getMessagePage(alice.id, conversationId, newest.olderCursor, 2).getOrThrow()
        assertEquals(listOf("message-2", "message-3"), middle.messages.map { it.id.value })
        val oldest = repository.getMessagePage(alice.id, conversationId, middle.olderCursor, 2).getOrThrow()
        assertEquals(listOf("message-1"), oldest.messages.map { it.id.value })
        assertEquals(null, oldest.olderCursor)
        assertEquals(
            emptyList(),
            repository.getMessagePage(
                alice.id,
                conversationId,
                before = com.jdrms.bulletin.domain.messages.domain.model.MessageCursor(
                    kotlin.time.Instant.fromEpochMilliseconds(0),
                    MessageId("any")
                ),
                limit = 2
            ).getOrThrow().messages
        )
    }

    private fun assertAccessDenied(result: Result<*>) {
        assertIs<ConversationAccessException>(assertIs<Result.Error>(result).exception)
    }
}

private fun Conversation.withListing(listingId: ListingReferenceId) =
    Conversation(id, participants, lastMessage, updatedAtMillis, listingId)

class InMemoryMessagesRepositoryTest : MessagesRepositoryContractTest() {
    override fun createRepository(conversations: List<Conversation>, messages: List<Message>): MessagesRepository =
        messagesRepository(conversations, messages, now = { Instant.fromEpochMilliseconds(42L) })

    @Test
    fun startingSameListingConversationAgainRestoresOnlyRequestersInbox() = runTest {
        val listingId = ListingReferenceId("listing-a")
        val repository = createRepository(
            listOf(
                Conversation(
                    conversationId,
                    listOf(
                        ConversationParticipant(alice.id, alice.displayName),
                        ConversationParticipant(carol.id, carol.displayName)
                    ),
                    listingId = listingId
                )
            )
        )
        repository.hideConversation(alice.id, conversationId).getOrThrow()
        repository.hideConversation(carol.id, conversationId).getOrThrow()

        val otherParticipant = ConversationParticipant(carol.id, carol.displayName)
        assertEquals(
            conversationId,
            repository.getOrCreateConversation(alice.id, otherParticipant, listingId).getOrThrow().id
        )
        assertEquals(listOf(conversationId), repository.getConversations(alice.id).getOrThrow().map { it.id })
        assertEquals(emptyList(), repository.getConversations(carol.id).getOrThrow())
    }

    @Test
    fun fetchedMessagesAreOrderedOldestToNewestWithStableTimestampTies() = runTest {
        val later = message(id = MessageId("later")).copy(timestampMillis = 100L)
        val tiedB = message(id = MessageId("b")).copy(timestampMillis = 42L)
        val tiedA = message(id = MessageId("a")).copy(timestampMillis = 42L)
        val repository = createRepository(listOf(conversation()), listOf(later, tiedB, tiedA))
        assertEquals(listOf(tiedA, tiedB, later), repository.getMessages(alice.id, conversationId).getOrThrow())
    }
}
