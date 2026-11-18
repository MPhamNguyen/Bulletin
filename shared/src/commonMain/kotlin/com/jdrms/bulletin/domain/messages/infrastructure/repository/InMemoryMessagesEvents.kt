package com.jdrms.bulletin.domain.messages.infrastructure.repository

import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import com.jdrms.bulletin.domain.messages.domain.repository.InboxInvalidation
import com.jdrms.bulletin.domain.messages.domain.repository.MessageStreamEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.merge

/** Publishes development-adapter changes using the repository's current access and visibility checks. */
internal class InMemoryMessagesEvents(
    private val canAccess: suspend (SenderId, ConversationId) -> Boolean,
    private val canReceive: suspend (SenderId, ConversationId, Message) -> Boolean
) {
    private val messages = MutableSharedFlow<Message>(extraBufferCapacity = 64)
    private val reads = MutableSharedFlow<InboxInvalidation.Changed>(extraBufferCapacity = 64)

    suspend fun messageChanged(message: Message) = messages.emit(message)

    suspend fun readChanged(conversationId: ConversationId) = reads.emit(InboxInvalidation.Changed(conversationId))

    fun observe(userId: SenderId, conversationId: ConversationId): Flow<MessageStreamEvent> = flow {
        if (!canAccess(userId, conversationId)) {
            emit(MessageStreamEvent.Failed(ConversationAccessException()))
            return@flow
        }
        emit(MessageStreamEvent.Connected)
        messages.collect { message ->
            if (canReceive(userId, conversationId, message)) emit(MessageStreamEvent.Received(message))
        }
    }

    fun observe(userId: SenderId): Flow<InboxInvalidation> = flow {
        emit(InboxInvalidation.Subscribed)
        merge(
            messages.mapNotNull { message ->
                if (canAccess(userId, message.conversationId)) {
                    InboxInvalidation.Changed(message.conversationId)
                } else {
                    null
                }
            },
            reads.mapNotNull { changed -> if (canAccess(userId, changed.conversationId)) changed else null }
        ).collect { emit(it) }
    }
}
