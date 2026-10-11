package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.repository.MessageStreamEvent
import com.jdrms.bulletin.domain.messages.infrastructure.repository.MessageRealtimeChannel
import com.jdrms.bulletin.domain.messages.infrastructure.repository.MessageRealtimeGateway
import com.jdrms.bulletin.domain.messages.infrastructure.repository.MessagesPersistenceException
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesObserver
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class SupabaseMessagesObserverTest {
    @Test
    fun subscriptionDeliversMessageAndClosesWhenCollectorStops() = runTest {
        val gateway = FakeMessageRealtimeGateway()
        val observer = SupabaseMessagesObserver(gateway, messagesRepository())
        val received = mutableListOf<MessageStreamEvent>()
        val job = backgroundScope.launch { observer.observe(alice.id, conversationId).toList(received) }
        runCurrent()

        gateway.channel.emit(MessageStreamEvent.Received(message()))
        runCurrent()
        job.cancel()
        runCurrent()

        assertEquals(conversationId, gateway.openedConversation)
        assertEquals(listOf(MessageStreamEvent.Connected, MessageStreamEvent.Received(message())), received)
        assertEquals(1, gateway.channel.closeCount)
    }

    @Test
    fun failedSubscribeRetriesWithBackoffBeforeConnecting() = runTest {
        val gateway = FakeMessageRealtimeGateway()
        gateway.channel.subscribeErrorsRemaining = 2
        val observer = SupabaseMessagesObserver(gateway, messagesRepository())
        val events = mutableListOf<MessageStreamEvent>()
        val job = backgroundScope.launch {
            observer.observe(alice.id, conversationId).take(3).toList(events)
        }

        advanceUntilIdle()
        job.join()

        assertEquals(
            listOf(MessageStreamEvent.Reconnecting, MessageStreamEvent.Reconnecting, MessageStreamEvent.Connected),
            events
        )
        assertEquals(3, gateway.channel.subscribeCount)
    }

    @Test
    fun disconnectedChannelIsReopenedWithBackoff() = runTest {
        val gateway = FakeMessageRealtimeGateway()
        val observer = SupabaseMessagesObserver(gateway, messagesRepository())
        val events = mutableListOf<MessageStreamEvent>()
        val job = backgroundScope.launch {
            observer.observe(alice.id, conversationId).toList(events)
        }
        runCurrent()

        gateway.channel.emit(MessageStreamEvent.Disconnected)
        runCurrent()
        advanceTimeBy(250)
        runCurrent()
        job.cancel()
        runCurrent()

        assertEquals(
            listOf(
                MessageStreamEvent.Connected,
                MessageStreamEvent.Disconnected,
                MessageStreamEvent.Reconnecting,
                MessageStreamEvent.Connected
            ),
            events
        )
        assertEquals(2, gateway.channel.subscribeCount)
    }

    @Test
    fun unauthenticatedUserCannotOpenChannel() = runTest {
        val gateway = FakeMessageRealtimeGateway(authenticatedId = null)
        val event = SupabaseMessagesObserver(gateway, messagesRepository()).observe(alice.id, conversationId).first()

        assertIs<ConversationAccessException>(assertIs<MessageStreamEvent.Failed>(event).cause)
        assertEquals(null, gateway.openedConversation)
    }

    @Test
    fun nonparticipantCannotOpenChannel() = runTest {
        val gateway = FakeMessageRealtimeGateway()
        val observer = SupabaseMessagesObserver(gateway, messagesRepository())
        val event = observer.observe(alice.id, ConversationId("missing")).first()

        assertIs<ConversationAccessException>(assertIs<MessageStreamEvent.Failed>(event).cause)
        assertEquals(null, gateway.openedConversation)
    }

    @Test
    fun subscriptionFailureIsMappedAndChannelClosed() = runTest {
        val gateway = FakeMessageRealtimeGateway()
        gateway.channel.subscribeError = IllegalStateException("private transport detail")
        val event = SupabaseMessagesObserver(gateway, messagesRepository())
            .observe(alice.id, conversationId)
            .first { it is MessageStreamEvent.Failed }

        assertIs<MessagesPersistenceException>(assertIs<MessageStreamEvent.Failed>(event).cause)
        assertEquals(4, gateway.channel.closeCount)
    }

    @Test
    fun downstreamCollectorExceptionPropagatesWithoutViolatingExceptionTransparency() = runTest {
        val gateway = FakeMessageRealtimeGateway()
        val observer = SupabaseMessagesObserver(gateway, messagesRepository())

        class DownstreamCollectorException : RuntimeException("Downstream collector threw")

        assertFailsWith<DownstreamCollectorException> {
            observer.observe(alice.id, conversationId).collect { event ->
                if (event is MessageStreamEvent.Connected) {
                    throw DownstreamCollectorException()
                }
            }
        }
        assertEquals(1, gateway.channel.closeCount)
    }

    @Test
    fun errorsAreNotCaughtOrConvertedToStreamFailure() = runTest {
        val gateway = FakeMessageRealtimeGateway()
        gateway.channel.subscribeError = AssertionError("Fatal VM assertion")
        val observer = SupabaseMessagesObserver(gateway, messagesRepository())

        assertFailsWith<AssertionError> {
            observer.observe(alice.id, conversationId).collect { }
        }
    }
}

private class FakeMessageRealtimeGateway(
    private val authenticatedId: String? = alice.id.value
) : MessageRealtimeGateway {
    val channel = FakeMessageRealtimeChannel()
    var openedConversation: ConversationId? = null
    var openCount = 0

    override suspend fun authenticatedUserId(): String? = authenticatedId

    override fun open(conversationId: ConversationId): MessageRealtimeChannel {
        openCount++
        openedConversation = conversationId
        return channel
    }
}

private class FakeMessageRealtimeChannel : MessageRealtimeChannel {
    private val changes = MutableSharedFlow<MessageStreamEvent>()
    override val events: Flow<MessageStreamEvent> = changes
    var subscribeError: Throwable? = null
    var subscribeErrorsRemaining = 0
    var subscribeCount = 0
    var closeCount = 0

    override suspend fun subscribe() {
        subscribeCount++
        if (subscribeErrorsRemaining > 0) {
            subscribeErrorsRemaining--
            error("Temporary subscription failure")
        }
        subscribeError?.let { throw it }
    }

    suspend fun emit(event: MessageStreamEvent) = changes.emit(event)

    override suspend fun close() {
        closeCount++
    }
}
