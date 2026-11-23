package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.GetConversations
import com.jdrms.bulletin.domain.messages.application.ObserveInboxInvalidations
import com.jdrms.bulletin.domain.messages.application.PendingConversationReview
import com.jdrms.bulletin.domain.messages.application.PendingConversationReviews
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ListingReferenceId
import com.jdrms.bulletin.domain.messages.domain.repository.InboxObserver
import com.jdrms.bulletin.domain.messages.presentation.InboxStateOwner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GetConversationsReviewsTest {
    @Test
    fun soldConversationMatchesOnlyTheBuyersListingAndSeller() = runTest {
        val chat = conversation()
        val repository = messagesRepository(
            listOf(
                Conversation(
                    chat.id,
                    chat.participants,
                    listingId = ListingReferenceId("listing:1"),
                    isListingSold = true
                )
            )
        )
        for ((listing, seller, expected) in listOf(
            Triple("1", carol.id, setOf(chat.id)),
            Triple("other", carol.id, emptySet()),
            Triple("1", bob.id, emptySet())
        )) {
            val get = GetConversations(
                repository,
                MutableMessageSenderProvider(),
                PendingConversationReviews { buyer ->
                    assertEquals(alice.id, buyer)
                    Result.Success(listOf(PendingConversationReview(ListingReferenceId(listing), seller)))
                }
            )
            val inbox = get().getOrThrow()
            assertEquals(expected, inbox.pendingReviewConversationIds)
            assertEquals(if (expected.isEmpty()) 1 else 0, inbox.unmatchedReviewRequests?.size)
        }
    }

    @Test
    fun deletedAccountsDoNotMatchPendingReviewsEvenWhenTheListingIsSold() = runTest {
        val request = PendingConversationReview(ListingReferenceId("1"), carol.id)
        val deletedAccountConversations = listOf(
            Conversation(
                conversationId,
                conversation(senders = listOf(alice)).participants,
                listingId = ListingReferenceId("1"),
                isListingSold = true
            ),
            Conversation(
                conversationId,
                conversation().participants,
                lastMessage = message(carol).copy(senderId = null),
                listingId = ListingReferenceId("1"),
                isListingSold = true
            )
        )
        for (chat in deletedAccountConversations) {
            val get = GetConversations(
                messagesRepository(listOf(chat)),
                MutableMessageSenderProvider(),
                PendingConversationReviews { Result.Success(listOf(request)) }
            )

            val inbox = get().getOrThrow()

            assertTrue(inbox.pendingReviewConversationIds.orEmpty().isEmpty())
            assertEquals(listOf(request), inbox.unmatchedReviewRequests)
        }
    }

    @Test
    fun hiddenConversationKeepsAnIndependentReviewEntryUntilCompletion() = runTest {
        val chat = Conversation(conversationId, conversation().participants, listingId = ListingReferenceId("1"))
        val repository = messagesRepository(listOf(chat))
        repository.hideConversation(alice.id, chat.id).getOrThrow()
        val request = PendingConversationReview(ListingReferenceId("1"), carol.id)
        var pending = listOf(request)
        val get = GetConversations(
            repository,
            MutableMessageSenderProvider(),
            PendingConversationReviews { Result.Success(pending) }
        )
        val inbox = get().getOrThrow()
        assertTrue(inbox.conversations.isEmpty())
        assertTrue(inbox.pendingReviewConversationIds.orEmpty().isEmpty())
        assertEquals(listOf(request), inbox.unmatchedReviewRequests)
        pending = emptyList()
        assertTrue(get().getOrThrow().unmatchedReviewRequests.orEmpty().isEmpty())
        assertTrue(repository.getConversations(alice.id).getOrThrow().isEmpty())
    }

    @Test
    fun unmatchedRequestsSurviveTemporaryFailureAndClearOnSignOut() = runTest {
        val request = PendingConversationReview(ListingReferenceId("1"), carol.id)
        var result: Result<List<PendingConversationReview>> = Result.Success(listOf(request))
        val identity = MutableMessageSenderProvider()
        val owner = InboxStateOwner(
            GetConversations(messagesRepository(emptyList()), identity, PendingConversationReviews { result }),
            ObserveInboxInvalidations(InboxObserver { emptyFlow() }),
            identity,
            backgroundScope
        )
        runCurrent()
        assertEquals(listOf(request), owner.state.value.unmatchedReviewRequests)
        result = Result.Error(IllegalStateException("Offline"))
        owner.refresh()
        runCurrent()
        assertEquals(listOf(request), owner.state.value.unmatchedReviewRequests)
        identity.result = Result.Error(IllegalStateException("Signed out"))
        runCurrent()
        assertTrue(owner.state.value.unmatchedReviewRequests.isEmpty())
        owner.close()
    }

    @Test
    fun temporaryReviewFailureRetainsReminderAndCompletionClearsItOnRefresh() = runTest {
        val chat = conversation()
        val repository = messagesRepository(
            listOf(
                Conversation(
                    chat.id,
                    chat.participants,
                    listingId = ListingReferenceId("1")
                )
            )
        )
        var result: Result<List<PendingConversationReview>> = Result.Success(
            listOf(PendingConversationReview(ListingReferenceId("1"), carol.id))
        )
        val identity = MutableMessageSenderProvider()
        val owner = InboxStateOwner(
            GetConversations(repository, identity, PendingConversationReviews { result }),
            ObserveInboxInvalidations(InboxObserver { emptyFlow() }),
            identity,
            backgroundScope
        )
        runCurrent()
        assertEquals(setOf(chat.id), owner.state.value.pendingReviewConversationIds)
        result = Result.Error(IllegalStateException("Offline"))
        owner.refresh()
        runCurrent()
        assertEquals(setOf(chat.id), owner.state.value.pendingReviewConversationIds)
        result = Result.Success(emptyList())
        owner.refresh()
        runCurrent()
        assertTrue(owner.state.value.pendingReviewConversationIds.isEmpty())
        owner.close()
    }
}
