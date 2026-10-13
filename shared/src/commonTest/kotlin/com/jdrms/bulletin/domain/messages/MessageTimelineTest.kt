package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.service.MessageTimeline
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class MessageTimelineTest {
    @Test
    fun mergeRetainsInsertDuringPageLoadAndDeduplicatesSendResponse() {
        val older = message(id = MessageId("older")).copy(timestampMillis = 1L)
        val sent = message(id = MessageId("sent")).copy(timestampMillis = 42L)
        val live = message(id = MessageId("live")).copy(timestampMillis = 43L)
        val afterEvent = MessageTimeline.merge(listOf(sent), listOf(live, sent))

        assertEquals(listOf(older, sent, live), MessageTimeline.merge(afterEvent, listOf(older, sent)))
    }

    @Test
    fun overlappingPageReplacesRealtimeFallbackMetadata() {
        val streamed = message().copy(senderName = "Student")
        val hydrated = streamed.copy(senderName = "Alice")

        assertEquals(listOf(hydrated), MessageTimeline.merge(listOf(streamed), listOf(hydrated)))
    }

    @Test
    fun realtimeFallbackDoesNotReplaceKnownSenderName() {
        val known = message().copy(senderName = "Alice")
        val fallbackEcho = known.copy(senderName = "Student")

        assertEquals(listOf(known), MessageTimeline.merge(listOf(known), listOf(fallbackEcho)))
        assertEquals(listOf(known), MessageTimeline.merge(listOf(fallbackEcho), listOf(known)))
    }

    @Test
    fun mergeUsesSubmillisecondTimestampBeforeId() {
        val early = message(id = MessageId("z")).copy(preciseCreatedAt = Instant.parse("2026-01-01T00:00:00.000100Z"))
        val late = message(id = MessageId("a")).copy(preciseCreatedAt = Instant.parse("2026-01-01T00:00:00.000200Z"))

        assertEquals(listOf(early, late), MessageTimeline.merge(listOf(late), listOf(early)))
    }
}
