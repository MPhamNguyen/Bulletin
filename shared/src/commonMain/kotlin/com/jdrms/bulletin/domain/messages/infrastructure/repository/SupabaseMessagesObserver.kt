package com.jdrms.bulletin.domain.messages.infrastructure.repository

import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.MessageStreamEvent
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesObserver
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesRepository
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

@Suppress("TooGenericExceptionCaught")
class SupabaseMessagesObserver internal constructor(
    private val gateway: MessageRealtimeGateway,
    private val repository: MessagesRepository
) : MessagesObserver {
    constructor(supabase: SupabaseClient, repository: MessagesRepository) :
        this(SupabaseMessageRealtimeGateway(supabase), repository)

    override fun observe(userId: SenderId, conversationId: ConversationId): Flow<MessageStreamEvent> = flow {
        var channel: MessageRealtimeChannel? = null
        try {
            if (gateway.authenticatedUserId() != userId.value) throw ConversationAccessException()
            repository.getMessagePage(userId, conversationId, limit = 1).getOrThrow()
            val active = gateway.open(conversationId)
            channel = active
            active.subscribe()
            emit(MessageStreamEvent.Connected)
            active.events.collect { emit(it) }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: ConversationAccessException) {
            emit(MessageStreamEvent.Failed(exception))
        } catch (exception: Throwable) {
            emit(MessageStreamEvent.Failed(MessagesPersistenceException(exception)))
        } finally {
            channel?.let { active ->
                withContext(NonCancellable) { runCatching { active.close() } }
            }
        }
    }
}
