package com.jdrms.bulletin.domain.messages.infrastructure.repository

import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.MessageStreamEvent
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesObserver
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Suppress("TooGenericExceptionCaught")
class SupabaseMessagesObserver internal constructor(
    private val gateway: MessageRealtimeGateway,
    private val repository: MessagesRepository
) : MessagesObserver {
    constructor(supabase: SupabaseClient, repository: MessagesRepository) :
        this(SupabaseMessageRealtimeGateway(supabase), repository)

    override fun observe(userId: SenderId, conversationId: ConversationId): Flow<MessageStreamEvent> = channelFlow {
        try {
            verifyAccess(userId, conversationId)
            observeWithRetries(conversationId)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: ConversationAccessException) {
            send(MessageStreamEvent.Failed(exception))
        } catch (exception: Exception) {
            send(MessageStreamEvent.Failed(MessagesPersistenceException(exception)))
        }
    }

    private suspend fun verifyAccess(
        userId: SenderId,
        conversationId: ConversationId
    ) {
        if (gateway.authenticatedUserId() != userId.value) throw ConversationAccessException()
        repository.getMessagePage(userId, conversationId, limit = 1).getOrThrow()
    }

    private suspend fun ProducerScope<MessageStreamEvent>.observeWithRetries(
        conversationId: ConversationId
    ) {
        var attempt = 0
        while (attempt < MAX_SUBSCRIBE_ATTEMPTS) {
            val failure = try {
                observeChannel(conversationId)
                IllegalStateException("Message stream ended unexpectedly")
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: ConversationAccessException) {
                send(MessageStreamEvent.Failed(exception))
                return
            } catch (exception: Exception) {
                exception
            }
            attempt++
            if (attempt == MAX_SUBSCRIBE_ATTEMPTS) {
                send(MessageStreamEvent.Failed(MessagesPersistenceException(failure)))
                return
            }
            send(MessageStreamEvent.Reconnecting)
            delay(SUBSCRIBE_RETRY_BASE_DELAY_MILLIS * (1L shl (attempt - 1)))
        }
    }

    private suspend fun ProducerScope<MessageStreamEvent>.observeChannel(
        conversationId: ConversationId
    ) {
        val channel = gateway.open(conversationId)
        try {
            coroutineScope {
                val connected = CompletableDeferred<Unit>()
                val eventsJob = launch(start = CoroutineStart.UNDISPATCHED) {
                    channel.events
                        .onEach {
                            connected.await()
                            send(it)
                        }
                        .takeWhile { it !is MessageStreamEvent.Disconnected }
                        .collect()
                }
                try {
                    channel.subscribe()
                    send(MessageStreamEvent.Connected)
                    connected.complete(Unit)
                    eventsJob.join()
                } finally {
                    eventsJob.cancel()
                }
            }
        } finally {
            withContext(NonCancellable) { runCatching { channel.close() } }
        }
    }
}

private const val MAX_SUBSCRIBE_ATTEMPTS = 4
private const val SUBSCRIBE_RETRY_BASE_DELAY_MILLIS = 250L
