package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.application.IncomingMessageAlert
import com.jdrms.bulletin.domain.messages.domain.model.MessageId

/**
 * Banners waiting to be shown; the head is the visible one. At most [capacity] are held. A new alert for a
 * conversation that already has a banner replaces it in place. A new conversation queues behind the visible
 * banner and, when the queue is full, displaces the oldest waiting banner rather than the visible one.
 */
internal class InAppBannerQueue(private val capacity: Int = DEFAULT_CAPACITY) {
    private val items = mutableListOf<IncomingMessageAlert>()

    val head: IncomingMessageAlert? get() = items.firstOrNull()

    fun offer(alert: IncomingMessageAlert) {
        val existing = items.indexOfFirst { it.conversationId == alert.conversationId }
        if (existing >= 0) {
            items[existing] = alert
            return
        }
        items += alert
        if (items.size > capacity) items.removeAt(1)
    }

    /** Returns false for a stale ID, such as one already replaced by a newer message. */
    fun dismiss(messageId: MessageId): Boolean = items.removeAll { it.messageId == messageId }

    fun clear() = items.clear()

    companion object {
        const val DEFAULT_CAPACITY = 2
    }
}
