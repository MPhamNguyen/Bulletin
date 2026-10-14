package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.GetConversations
import com.jdrms.bulletin.domain.messages.application.ObserveInboxInvalidations
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.InboxInvalidation
import com.jdrms.bulletin.domain.messages.domain.repository.InboxObserver
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import com.jdrms.bulletin.domain.messages.presentation.InboxConnectionState
import com.jdrms.bulletin.domain.messages.presentation.InboxStateOwner
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class InboxStateOwnerTest {
    @Test
    fun burstsOfChangesCoalesceIntoOneAuthorizedRefresh() = runTest {
        val backing = messagesRepository()
        var loads = 0
        val repository = object : MessagesRepository by backing {
            override suspend fun getConversations(userId: SenderId): Result<List<Conversation>> {
                loads++
                return backing.getConversations(userId)
            }
        }
        val events = MutableSharedFlow<InboxInvalidation>(extraBufferCapacity = 4)
        val provider = MutableMessageSenderProvider()
        val owner = InboxStateOwner(
            GetConversations(repository, provider),
            ObserveInboxInvalidations(InboxObserver { events }),
            provider,
            backgroundScope
        )
        runCurrent()
        assertEquals(1, loads)

        events.emit(InboxInvalidation.Changed(conversationId))
        advanceTimeBy(100)
        events.emit(InboxInvalidation.Changed(conversationId))
        advanceTimeBy(299)
        runCurrent()
        assertEquals(1, loads)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(2, loads)
        owner.close()
    }

    @Test
    fun resubscribeAndForegroundReloadMissedMessages() = runTest {
        val backing = messagesRepository()
        val events = MutableSharedFlow<InboxInvalidation>(extraBufferCapacity = 4)
        val provider = MutableMessageSenderProvider()
        val owner = InboxStateOwner(
            GetConversations(backing, provider),
            ObserveInboxInvalidations(InboxObserver { events }),
            provider,
            backgroundScope
        )
        runCurrent()
        assertNull(owner.state.value.conversations.single().lastMessage)
        backing.sendMessage(message())
        events.emit(InboxInvalidation.Subscribed)
        runCurrent()
        assertEquals(InboxConnectionState.CONNECTED, owner.state.value.connectionState)
        assertEquals(message(), owner.state.value.conversations.single().lastMessage)

        val newer = message(id = MessageId("newer")).copy(timestampMillis = 100L)
        backing.sendMessage(newer)
        owner.onForeground()
        runCurrent()
        assertEquals(newer, owner.state.value.conversations.single().lastMessage)
        owner.close()
    }

    @Test
    fun switchingAccountsDiscardsThePreviousInboxBeforeLoadingTheNext() = runTest {
        val other = conversation(ConversationId("other"), listOf(bob, carol))
        val backing = messagesRepository(listOf(conversation(), other), listOf(message()))
        val provider = MutableMessageSenderProvider()
        val owner = InboxStateOwner(
            GetConversations(backing, provider),
            ObserveInboxInvalidations(InboxObserver { MutableSharedFlow() }),
            provider,
            backgroundScope
        )
        runCurrent()
        assertEquals(listOf(conversationId), owner.state.value.conversations.map { it.id })

        provider.result = Result.Success(bob)
        owner.synchronizeIdentity()
        assertEquals(emptyList(), owner.state.value.conversations)
        runCurrent()
        assertEquals(listOf(other.id), owner.state.value.conversations.map { it.id })
        owner.close()
    }

    @Test
    fun signingOutClearsPrivateInboxAndStopsOldObservation() = runTest {
        val backing = messagesRepository()
        val provider = MutableMessageSenderProvider()
        var cancelled = false
        val observer = InboxObserver {
            kotlinx.coroutines.flow.flow {
                emit(InboxInvalidation.Subscribed)
                try { kotlinx.coroutines.awaitCancellation() } finally { cancelled = true }
            }
        }
        val owner = InboxStateOwner(
            GetConversations(backing, provider),
            ObserveInboxInvalidations(observer),
            provider,
            backgroundScope
        )
        runCurrent()
        assertEquals(alice.id, owner.state.value.viewerId)
        provider.result = Result.Error(IllegalStateException("signed out"))
        runCurrent()

        assertNull(owner.state.value.viewerId)
        assertEquals(emptyList(), owner.state.value.conversations)
        assertFalse(owner.state.value.isLoading)
        assertEquals(true, cancelled)
        owner.close()
    }
}
