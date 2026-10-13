package com.jdrms.bulletin.domain.messages.domain.service

import com.jdrms.bulletin.domain.messages.domain.model.Message

/** Reconciles overlapping page responses, send responses, and stream inserts. */
object MessageTimeline {
    fun merge(current: List<Message>, incoming: List<Message>): List<Message> =
        (current + incoming).groupBy(Message::id).values.map(::preferKnownSenderName)
            .sortedWith(compareBy<Message> { it.orderingTimestamp }.thenBy { it.id.value })

    private fun preferKnownSenderName(versions: List<Message>): Message {
        val latest = versions.last()
        val knownName = versions.lastOrNull { it.senderName != DEFAULT_SENDER_NAME }?.senderName
        return if (latest.senderName == DEFAULT_SENDER_NAME && knownName != null) {
            latest.copy(senderName = knownName)
        } else {
            latest
        }
    }

    private const val DEFAULT_SENDER_NAME = "Student"
}
