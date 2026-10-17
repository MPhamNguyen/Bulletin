package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.MessagesInstrumentation
import com.jdrms.bulletin.domain.messages.application.MessagesMeasurement
import com.jdrms.bulletin.domain.messages.application.MessagesTimingEvent
import com.jdrms.bulletin.domain.messages.application.measuredMessagesOperation
import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertSame

class MessagesInstrumentationTest {
    @Test
    fun recordsOperationDurationCountAndSafeAccessCategory() = runTest {
        val events = mutableListOf<MessagesTimingEvent>()
        val instrumentation = MessagesInstrumentation(events::add)
        val accessError = ConversationAccessException()

        val result = measuredMessagesOperation(instrumentation, "membership_check") {
            Result.Error(accessError)
        }

        assertEquals("membership_check", events.single().operation)
        assertEquals(1, events.single().callCount)
        assertEquals(null, events.single().resultCount)
        assertEquals("access_denied", events.single().outcome)
        assertSame(accessError, assertIs<Result.Error>(result).exception)
    }

    @Test
    fun recordsSuccessfulResultCount() = runTest {
        val events = mutableListOf<MessagesTimingEvent>()

        val result = measuredMessagesOperation(MessagesInstrumentation(events::add), "inbox_retrieval", { it.size }) {
            listOf("one", "two")
        }

        assertEquals(2, result.size)
        assertEquals(1, events.single().callCount)
        assertEquals(2, events.single().resultCount)
        assertEquals("success", events.single().outcome)
    }

    @Test
    fun recordsUtf8PayloadSizeAndMicrosecondDuration() = runTest {
        val events = mutableListOf<MessagesTimingEvent>()

        measuredMessagesOperation(
            MessagesInstrumentation(events::add),
            "inbox_request_body",
            MessagesMeasurement(responseBytes = { it.encodeToByteArray().size })
        ) { "é" }

        assertEquals(2, events.single().responseBytes)
        assertEquals(events.single().durationMicros / 1_000, events.single().durationMillis)
    }

    @Test
    fun recordsCancellationAndRethrowsIt() = runTest {
        val events = mutableListOf<MessagesTimingEvent>()
        val cancellation = CancellationException("test")

        assertFailsWith<CancellationException> {
            measuredMessagesOperation(MessagesInstrumentation(events::add), "message_fetch") {
                throw cancellation
            }
        }

        assertEquals(null, events.single().callCount)
        assertEquals("cancelled", events.single().outcome)
    }
}
