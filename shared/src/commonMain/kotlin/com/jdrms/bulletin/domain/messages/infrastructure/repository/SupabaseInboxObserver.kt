package com.jdrms.bulletin.domain.messages.infrastructure.repository

import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.InboxInvalidation
import com.jdrms.bulletin.domain.messages.domain.repository.InboxObserver
import com.jdrms.bulletin.domain.messages.infrastructure.mapper.SupabaseInboxInvalidationMapper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.broadcastFlow
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.ProducerScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

internal interface InboxRealtimeGateway {
    suspend fun authenticatedUserId(): String?
    fun open(userId: SenderId): InboxRealtimeChannel
}

internal interface InboxRealtimeChannel {
    val events: Flow<InboxInvalidation>
    suspend fun subscribe()
    suspend fun close()
}

internal class SupabaseInboxRealtimeGateway(private val supabase: SupabaseClient) : InboxRealtimeGateway {
    override suspend fun authenticatedUserId(): String? {
        supabase.auth.awaitInitialization()
        return supabase.auth.currentUserOrNull()?.id
    }

    override fun open(userId: SenderId): InboxRealtimeChannel {
        val channel = supabase.channel("inbox:${userId.value}") { isPrivate = true }
        return SupabaseInboxRealtimeChannel(supabase, channel)
    }
}

private class SupabaseInboxRealtimeChannel(
    private val supabase: SupabaseClient,
    private val channel: RealtimeChannel
) : InboxRealtimeChannel {
    private val changes = channel.broadcastFlow<JsonObject>("conversation-changed").mapNotNull { payload ->
        SupabaseInboxInvalidationMapper.conversationIdOrNull(payload)?.let(InboxInvalidation::Changed)
    }
    private val status = channel.status.drop(1).mapNotNull(::invalidationForChannelStatus)

    override val events: Flow<InboxInvalidation> = merge(changes, status)

    override suspend fun subscribe() = channel.subscribe(blockUntilSubscribed = true)

    override suspend fun close() = supabase.realtime.removeChannel(channel)
}

class SupabaseInboxObserver internal constructor(private val gateway: InboxRealtimeGateway) : InboxObserver {
    constructor(supabase: SupabaseClient) : this(SupabaseInboxRealtimeGateway(supabase))

    override fun observe(userId: SenderId): Flow<InboxInvalidation> = channelFlow {
        val authenticatedId = runCatching { gateway.authenticatedUserId() }.getOrElse { failure ->
            if (failure is CancellationException) throw failure
            send(InboxInvalidation.Failed(MessagesPersistenceException(failure)))
            return@channelFlow
        }
        if (authenticatedId != userId.value) {
            send(InboxInvalidation.Failed(ConversationAccessException()))
            return@channelFlow
        }
        observeWithRetries(userId)
    }

    private suspend fun ProducerScope<InboxInvalidation>.observeWithRetries(userId: SenderId) {
        repeat(MAX_INBOX_SUBSCRIBE_ATTEMPTS) { attempt ->
            val failure = runCatching { observeChannel(userId) }.exceptionOrNull()
                ?: IllegalStateException("Inbox stream ended unexpectedly")
            if (failure is CancellationException) throw failure
            if (attempt == MAX_INBOX_SUBSCRIBE_ATTEMPTS - 1) {
                send(InboxInvalidation.Failed(MessagesPersistenceException(failure)))
                return
            }
            send(InboxInvalidation.Disconnected)
            delay(INBOX_RETRY_BASE_DELAY_MILLIS * (1L shl attempt))
        }
    }

    private suspend fun ProducerScope<InboxInvalidation>.observeChannel(userId: SenderId) {
        val channel = gateway.open(userId)
        try {
            coroutineScope {
                val events = launch(start = CoroutineStart.UNDISPATCHED) {
                    channel.events.takeWhile { it !is InboxInvalidation.Disconnected }.collect { send(it) }
                }
                channel.subscribe()
                send(InboxInvalidation.Subscribed)
                events.join()
            }
        } finally {
            withContext(NonCancellable) { runCatching { channel.close() } }
        }
    }
}

internal fun invalidationForChannelStatus(status: RealtimeChannel.Status): InboxInvalidation? = when (status) {
    RealtimeChannel.Status.SUBSCRIBED -> InboxInvalidation.Subscribed
    RealtimeChannel.Status.UNSUBSCRIBED,
    RealtimeChannel.Status.UNSUBSCRIBING -> InboxInvalidation.Disconnected
    RealtimeChannel.Status.SUBSCRIBING -> null
}

private const val MAX_INBOX_SUBSCRIBE_ATTEMPTS = 4
private const val INBOX_RETRY_BASE_DELAY_MILLIS = 250L
