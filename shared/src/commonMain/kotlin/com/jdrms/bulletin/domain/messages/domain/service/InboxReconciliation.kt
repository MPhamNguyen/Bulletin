package com.jdrms.bulletin.domain.messages.domain.service

import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.Message

/** Preserve a newer acknowledged message while an older inbox fetch completes. */
object InboxReconciliation {
    fun merge(current: List<Conversation>, fetched: List<Conversation>): List<Conversation> {
        val currentById = current.associateBy(Conversation::id)
        return fetched.map { remote ->
            val previous = currentById[remote.id]?.lastMessage
            if (previous != null && previous.isNewerThan(remote.lastMessage)) {
                remote.recordMessage(previous).getOrNull() ?: remote
            } else {
                remote
            }
        }.sortedByDescending(Conversation::updatedAtMillis)
    }

    private fun Message.isNewerThan(other: Message?): Boolean = other == null ||
        orderingTimestamp > other.orderingTimestamp ||
        (orderingTimestamp == other.orderingTimestamp && id.value > other.id.value)
}
