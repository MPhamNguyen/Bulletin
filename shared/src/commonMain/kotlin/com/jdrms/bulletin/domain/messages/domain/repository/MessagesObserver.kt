package com.jdrms.bulletin.domain.messages.domain.repository

import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import kotlinx.coroutines.flow.Flow

sealed interface MessageStreamEvent {
    data object Connected : MessageStreamEvent
    data object Disconnected : MessageStreamEvent
    data object Reconnecting : MessageStreamEvent
    data class Received(val message: Message) : MessageStreamEvent
    data class Failed(val cause: Throwable) : MessageStreamEvent
}

fun interface MessagesObserver {
    fun observe(userId: SenderId, conversationId: ConversationId): Flow<MessageStreamEvent>
}
