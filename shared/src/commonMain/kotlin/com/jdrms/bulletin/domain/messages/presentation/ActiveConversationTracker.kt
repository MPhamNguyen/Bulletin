package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.domain.model.ConversationId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Which conversation the viewer is looking at right now. A conversation is only "being looked at" while the app
 * is in the foreground; this is a local signal, not server presence.
 */
class ActiveConversationTracker {
    private val openConversation = MutableStateFlow<ConversationId?>(null)
    private val isAppForeground = MutableStateFlow(false)

    val suppressedConversation: Flow<ConversationId?> =
        combine(openConversation, isAppForeground) { open, foreground -> if (foreground) open else null }
            .distinctUntilChanged()

    fun setOpenConversation(conversationId: ConversationId?) {
        openConversation.value = conversationId
    }

    fun setAppForeground(isForeground: Boolean) {
        isAppForeground.value = isForeground
    }
}
