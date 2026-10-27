package com.jdrms.bulletin.domain.messages.infrastructure.repository

import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.repository.MessageStreamEvent
import com.jdrms.bulletin.domain.messages.infrastructure.mapper.SupabaseRealtimeMessageMapper
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.RealtimeChannel
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge

internal interface MessageRealtimeGateway {
    suspend fun authenticatedUserId(): String?
    fun open(conversationId: ConversationId): MessageRealtimeChannel
}

internal interface MessageRealtimeChannel {
    val events: Flow<MessageStreamEvent>
    suspend fun subscribe()
    suspend fun close()
}

internal class SupabaseMessageRealtimeGateway(private val supabase: SupabaseClient) : MessageRealtimeGateway {
    override suspend fun authenticatedUserId(): String? {
        supabase.auth.awaitInitialization()
        return supabase.auth.currentUserOrNull()?.id
    }

    override fun open(conversationId: ConversationId): MessageRealtimeChannel {
        val channel = supabase.channel("messages:${conversationId.value}")
        return SupabaseMessageRealtimeChannel(supabase, channel, conversationId)
    }
}

private class SupabaseMessageRealtimeChannel(
    private val supabase: SupabaseClient,
    private val channel: RealtimeChannel,
    conversationId: ConversationId
) : MessageRealtimeChannel {
    private val inserts = channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
        table = SupabaseMessagesRepository.MESSAGES_TABLE
        filter("conversation_id", FilterOperator.EQ, conversationId.value)
    }.mapNotNull { action ->
        SupabaseRealtimeMessageMapper.toMessageOrNull(action.record, conversationId)
            ?.let(MessageStreamEvent::Received)
    }

    private val updates = channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
        table = SupabaseMessagesRepository.MESSAGES_TABLE
        filter("conversation_id", FilterOperator.EQ, conversationId.value)
    }.mapNotNull { action ->
        SupabaseRealtimeMessageMapper.toMessageOrNull(action.record, conversationId)
            ?.let(MessageStreamEvent::Received)
    }

    private val status = channel.status.drop(1).map { it.toMessageStreamEvent() }

    override val events: Flow<MessageStreamEvent> = merge(inserts, updates, status)

    override suspend fun subscribe() = channel.subscribe(blockUntilSubscribed = true)

    override suspend fun close() = supabase.realtime.removeChannel(channel)
}

internal fun RealtimeChannel.Status.toMessageStreamEvent(): MessageStreamEvent = when (this) {
    RealtimeChannel.Status.SUBSCRIBED -> MessageStreamEvent.Connected
    RealtimeChannel.Status.SUBSCRIBING -> MessageStreamEvent.Reconnecting
    RealtimeChannel.Status.UNSUBSCRIBED,
    RealtimeChannel.Status.UNSUBSCRIBING -> MessageStreamEvent.Disconnected
}
