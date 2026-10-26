package com.jdrms.bulletin.domain.messages.domain.service

import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.domain.model.Message

/** Preserve a newer acknowledged message while an older inbox fetch completes. */
object InboxReconciliation {
    fun merge(current: List<Conversation>, fetched: List<Conversation>): List<Conversation> {
        val currentById = current.associateBy(Conversation::id)
        return fetched.map { remote ->
            val previousConversation = currentById[remote.id] ?: return@map remote
            val withCurrentReads = previousConversation.participants.fold(remote) { merged, participant ->
                val previousReadAt = participant.lastReadAt
                if (previousReadAt != null) {
                    merged.recordReadAt(participant.id, previousReadAt).getOrNull() ?: merged
                } else {
                    merged
                }
            }
            val previous = previousConversation.lastMessage
            if (previous != null && previous.isNewerThan(remote.lastMessage)) {
                withCurrentReads.recordMessage(previous).getOrNull() ?: withCurrentReads
            } else {
                withCurrentReads
            }
        }.sortedByDescending(Conversation::updatedAtMillis)
    }

    private fun Message.isNewerThan(other: Message?): Boolean = other == null ||
        orderingTimestamp > other.orderingTimestamp ||
        (orderingTimestamp == other.orderingTimestamp && id.value > other.id.value) ||
        (id == other.id && editedAt != null && (other.editedAt == null || editedAt > other.editedAt))
}
