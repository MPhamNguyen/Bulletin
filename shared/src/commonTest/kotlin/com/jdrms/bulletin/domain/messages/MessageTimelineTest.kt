package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.service.MessageTimeline
import kotlin.test.Test
import kotlin.test.assertEquals

class MessageTimelineTest {
    @Test
    fun mergeRetainsInsertDuringPageLoadAndDeduplicatesSendResponse() {
        val older = message(id = MessageId("older")).copy(timestampMillis = 1L)
        val sent = message(id = MessageId("sent")).copy(timestampMillis = 42L)
        val live = message(id = MessageId("live")).copy(timestampMillis = 43L)
        val afterEvent = MessageTimeline.merge(listOf(sent), listOf(live, sent))

        assertEquals(listOf(older, sent, live), MessageTimeline.merge(afterEvent, listOf(older, sent)))
    }
}
