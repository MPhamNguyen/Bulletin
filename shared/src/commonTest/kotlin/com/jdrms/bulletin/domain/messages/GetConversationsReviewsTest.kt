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
    fun matchesOnlyTheBuyersListingAndSeller() = runTest {
        val chat = conversation()
        val repository = messagesRepository(
            listOf(
                Conversation(
                    chat.id,
                    chat.participants,
                    listingId = ListingReferenceId("listing:1")
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
            assertEquals(expected, get().getOrThrow().pendingReviewConversationIds)
        }
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
