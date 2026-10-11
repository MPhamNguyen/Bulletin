package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.application.MessageSenderLookupException
import com.jdrms.bulletin.domain.messages.application.MessagingAuthenticationRequiredException
import com.jdrms.bulletin.domain.messages.application.ObserveConversationMessages
import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.Message
import com.jdrms.bulletin.domain.messages.domain.repository.MessageStreamEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

internal class MessageStreamCallbacks(
    val isCurrent: () -> Boolean,
    val onConnected: () -> Unit,
    val onReceived: (Message) -> Unit,
    val onFailed: (Throwable, Boolean) -> Unit
)

internal class MessageStreamCoordinator(
    private val observeConversationMessages: ObserveConversationMessages
) {
    fun observe(
        scope: CoroutineScope,
        conversationId: ConversationId,
        callbacks: MessageStreamCallbacks
    ): Job = scope.launch {
        observeConversationMessages(conversationId).collect { event ->
            if (!callbacks.isCurrent()) return@collect
            when (event) {
                MessageStreamEvent.Connected -> callbacks.onConnected()
                is MessageStreamEvent.Received -> callbacks.onReceived(event.message)
                is MessageStreamEvent.Failed -> callbacks.onFailed(event.cause, event.cause.isTerminal())
            }
        }
    }

    private fun Throwable.isTerminal(): Boolean =
        this is MessagingAuthenticationRequiredException ||
            this is MessageSenderLookupException ||
            this is ConversationAccessException
}
