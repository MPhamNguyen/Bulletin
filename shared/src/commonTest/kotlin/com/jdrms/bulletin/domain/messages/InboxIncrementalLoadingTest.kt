package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.GetConversations
import com.jdrms.bulletin.domain.messages.application.ObserveInboxInvalidations
import com.jdrms.bulletin.domain.messages.application.PendingConversationReviews
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.InboxInvalidation
import com.jdrms.bulletin.domain.messages.domain.repository.InboxObserver
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.presentation.InboxStateOwner
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InboxIncrementalLoadingTest {
    @Test
    fun pagesPublishWhileReviewsAreSuspendedAndStopAtTheEnd() = runTest {
        val identity = MutableMessageSenderProvider()
        val reviews = CompletableDeferred<Unit>()
        val owner = InboxStateOwner(
            GetConversations(
                messagesRepository((1..23).map { conversation(ConversationId("chat-$it")) }),
                identity,
                PendingConversationReviews {
                    reviews.await()
                    Result.Success(emptyList())
                }
            ),
            ObserveInboxInvalidations(InboxObserver { MutableSharedFlow() }),
            identity,
            backgroundScope
        )
        runCurrent()
        assertEquals(10, owner.state.value.conversations.size)
        assertFalse(owner.state.value.isLoading)
        owner.loadMore()
        owner.loadMore()
        runCurrent()
        assertEquals(20, owner.state.value.conversations.size)
        owner.loadMore()
        runCurrent()
        assertEquals(23, owner.state.value.conversations.size)
        assertFalse(owner.state.value.hasMore)
        assertFalse(owner.state.value.isLoadingMore)
        reviews.complete(Unit)
        runCurrent()
    }

    @Test
    fun changedEventFetchesOnlyTheAffectedConversation() = runTest {
        val identity = MutableMessageSenderProvider()
        val events = MutableSharedFlow<InboxInvalidation>()
        val delegate = messagesRepository()
        var pageCalls = 0
        val refreshed = mutableListOf<ConversationId>()
        val repository = object : MessagesRepository by delegate {
            override suspend fun getConversationPage(userId: SenderId, offset: Int, limit: Int) =
                delegate.getConversationPage(userId, offset, limit).also { pageCalls++ }
            override suspend fun getConversation(userId: SenderId, conversationId: ConversationId) =
                delegate.getConversation(userId, conversationId).also { refreshed += conversationId }
        }
        val owner = InboxStateOwner(
            GetConversations(repository, identity),
            ObserveInboxInvalidations(InboxObserver { events }),
            identity,
            backgroundScope
        )
        runCurrent()
        val initialCalls = pageCalls
        events.emit(InboxInvalidation.Changed(conversationId))
        runCurrent()
        assertEquals(listOf(conversationId), refreshed)
        assertEquals(initialCalls, pageCalls)
        assertTrue(owner.state.value.conversations.isNotEmpty())
    }
}
