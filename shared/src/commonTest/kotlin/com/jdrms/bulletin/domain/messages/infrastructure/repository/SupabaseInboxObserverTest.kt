package com.jdrms.bulletin.domain.messages.infrastructure.repository

import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.InboxInvalidation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class SupabaseInboxObserverTest {
    private val user = SenderId("alice")

    @Test
    fun authenticatedUserReceivesPrivateInvalidationAndChannelClosesOnCancel() = runTest {
        val gateway = FakeInboxGateway(user.value)
        val events = mutableListOf<InboxInvalidation>()
        val job = backgroundScope.launch { SupabaseInboxObserver(gateway).observe(user).take(2).toList(events) }
        runCurrent()
        gateway.channels.single().events.emit(InboxInvalidation.Changed(ConversationId("conversation")))
        runCurrent()

        assertEquals(
            listOf(InboxInvalidation.Subscribed, InboxInvalidation.Changed(ConversationId("conversation"))),
            events
        )
        job.cancel()
        runCurrent()
        assertEquals(1, gateway.channels.single().closes)
    }

    @Test
    fun invalidationDuringSubscribeIsObserved() = runTest {
        val gateway = FakeInboxGateway(user.value)
        gateway.onChannelOpen = { channel ->
            channel.onSubscribe = {
                channel.events.emit(InboxInvalidation.Changed(ConversationId("first-message")))
            }
        }
        val events = mutableListOf<InboxInvalidation>()
        backgroundScope.launch { SupabaseInboxObserver(gateway).observe(user).take(2).toList(events) }
        runCurrent()

        assertEquals(
            setOf(InboxInvalidation.Changed(ConversationId("first-message")), InboxInvalidation.Subscribed),
            events.toSet()
        )
    }

    @Test
    fun anotherUsersIdentityCannotOpenInboxChannel() = runTest {
        val gateway = FakeInboxGateway("someone-else")
        val failure = SupabaseInboxObserver(gateway).observe(user).first()
        assertIs<ConversationAccessException>(assertIs<InboxInvalidation.Failed>(failure).cause)
        assertEquals(0, gateway.channels.size)
    }

    @Test
    fun disconnectClosesChannelAndRetriesWithBoundedBackoff() = runTest {
        val gateway = FakeInboxGateway(user.value)
        val events = mutableListOf<InboxInvalidation>()
        val job = backgroundScope.launch { SupabaseInboxObserver(gateway).observe(user).take(3).toList(events) }
        runCurrent()
        gateway.channels.single().events.emit(InboxInvalidation.Disconnected)
        runCurrent()
        assertEquals(1, gateway.channels.single().closes)
        advanceTimeBy(250)
        runCurrent()
        assertEquals(2, gateway.channels.size)
        assertEquals(2, events.count { it is InboxInvalidation.Subscribed })
        job.cancel()
    }
}

private class FakeInboxGateway(private val userId: String?) : InboxRealtimeGateway {
    val channels = mutableListOf<FakeInboxChannel>()
    var onChannelOpen: (FakeInboxChannel) -> Unit = {}
    override suspend fun authenticatedUserId(): String? = userId
    override fun open(userId: SenderId): InboxRealtimeChannel = FakeInboxChannel().also {
        channels.add(it)
        onChannelOpen(it)
    }
}

private class FakeInboxChannel : InboxRealtimeChannel {
    override val events: MutableSharedFlow<InboxInvalidation> = MutableSharedFlow(extraBufferCapacity = 4)
    var closes = 0
    var onSubscribe: suspend () -> Unit = {}
    override suspend fun subscribe() = onSubscribe()
    override suspend fun close() { closes++ }
}
