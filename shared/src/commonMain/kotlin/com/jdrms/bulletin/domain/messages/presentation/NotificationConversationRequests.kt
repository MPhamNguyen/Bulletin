package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class NotificationConversationRequest(val conversationId: ConversationId, val recipientId: SenderId)

/**
 * Holds a notification tap until the intended user is signed in. A request addressed to a different account is
 * dropped rather than opened, and a payload never grants access: the opened conversation must still load for
 * the current viewer.
 */
internal class NotificationConversationRequests {
    private val mutablePending = MutableStateFlow<NotificationConversationRequest?>(null)
    val pending: StateFlow<NotificationConversationRequest?> = mutablePending.asStateFlow()

    fun submit(request: NotificationConversationRequest) {
        mutablePending.value = request
    }

    /** Returns the conversation to open for [viewerId], keeping the request while nobody is signed in. */
    fun take(viewerId: SenderId?): ConversationId? {
        val request = mutablePending.value ?: return null
        if (viewerId == null) return null
        mutablePending.value = null
        return request.conversationId.takeIf { request.recipientId == viewerId }
    }
}
