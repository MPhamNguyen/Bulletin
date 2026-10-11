package com.jdrms.bulletin.domain.messages.domain.service

import com.jdrms.bulletin.domain.messages.domain.model.Message

/** Reconciles overlapping page responses, send responses, and stream inserts. */
object MessageTimeline {
    fun merge(current: List<Message>, incoming: List<Message>): List<Message> =
        (current + incoming).distinctBy(Message::id)
            .sortedWith(compareBy<Message> { it.timestampMillis }.thenBy { it.id.value })
}
