package com.jdrms.bulletin.domain.messages.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.repository.MessageStreamEvent
import com.jdrms.bulletin.domain.messages.domain.repository.MessagesObserver
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

class ObserveConversationMessages(
    private val observer: MessagesObserver,
    private val currentSenderProvider: CurrentMessageSenderProvider
) {
    operator fun invoke(conversationId: ConversationId): Flow<MessageStreamEvent> = flow {
        when (val sender = currentSenderProvider.getCurrentSender()) {
            is Result.Success -> emitAll(observer.observe(sender.data.id, conversationId))
            is Result.Error -> emit(MessageStreamEvent.Failed(sender.exception))
        }
    }
}
