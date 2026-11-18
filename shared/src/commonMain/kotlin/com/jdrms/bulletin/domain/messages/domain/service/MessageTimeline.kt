package com.jdrms.bulletin.domain.messages.domain.service

import com.jdrms.bulletin.domain.messages.domain.model.Message

/** Reconciles overlapping page responses, send responses, and stream inserts. */
object MessageTimeline {
    fun merge(current: List<Message>, incoming: List<Message>): List<Message> =
        (current + incoming).groupBy(Message::id).values.map(::preferKnownSenderName)
            .sortedWith(compareBy<Message> { it.orderingTimestamp }.thenBy { it.id.value })

    private fun preferKnownSenderName(versions: List<Message>): Message {
        val newestEdit = versions.mapNotNull { it.editedAt }.maxOrNull()
        val latest = versions.last { it.editedAt == newestEdit }
        // A delayed page or edit response must never reattach a hard-deleted sender.
        if (versions.any { it.senderId == null }) {
            return preserveReportState(latest.copy(senderId = null, senderName = Message.MISSING_SENDER_NAME), versions)
        }
        val knownName = versions.lastOrNull { it.senderName != DEFAULT_SENDER_NAME }?.senderName
        val resolved = if (latest.senderName == DEFAULT_SENDER_NAME && knownName != null) {
            latest.copy(senderName = knownName)
        } else {
            latest
        }
        return preserveReportState(resolved, versions)
    }

    private fun preserveReportState(message: Message, versions: List<Message>): Message =
        message.copy(
            isReported = versions.any { it.isReported },
            isEditLocked = versions.lastOrNull { it.isEditLocked != null }?.isEditLocked
        )

    private const val DEFAULT_SENDER_NAME = "Student"
}
