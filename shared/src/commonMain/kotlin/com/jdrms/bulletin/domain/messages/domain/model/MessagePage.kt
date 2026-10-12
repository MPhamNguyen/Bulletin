package com.jdrms.bulletin.domain.messages.domain.model

import kotlin.time.Instant

/** The database ordering key. Preserve the full timestamp precision across page boundaries. */
data class MessageCursor(val createdAt: Instant, val id: MessageId)

data class MessagePage(
    val messages: List<Message>,
    val olderCursor: MessageCursor?
)

const val MESSAGE_PAGE_SIZE = 30
