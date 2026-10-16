package com.jdrms.bulletin.domain.messages.domain.repository

import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import kotlinx.coroutines.flow.Flow

sealed interface InboxInvalidation {
    data object Subscribed : InboxInvalidation
    data object Disconnected : InboxInvalidation
    data class Changed(val conversationId: ConversationId) : InboxInvalidation
    data class Failed(val cause: Throwable) : InboxInvalidation
}

fun interface InboxObserver {
    fun observe(userId: SenderId): Flow<InboxInvalidation>
}
