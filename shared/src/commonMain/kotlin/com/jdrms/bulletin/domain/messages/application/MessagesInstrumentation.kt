package com.jdrms.bulletin.domain.messages.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import kotlin.time.TimeSource

data class MessagesTimingEvent(
    val operation: String,
    val durationMillis: Long,
    val callCount: Int?,
    val resultCount: Int?,
    val outcome: String,
    val responseBytes: Int? = null,
    val durationMicros: Long = durationMillis * 1_000
)

fun interface MessagesInstrumentation {
    fun record(event: MessagesTimingEvent)
}

object NoOpMessagesInstrumentation : MessagesInstrumentation {
    override fun record(event: MessagesTimingEvent) = Unit
}

object ConsoleMessagesInstrumentation : MessagesInstrumentation {
    override fun record(event: MessagesTimingEvent) {
        println(
            "messages operation=${event.operation} duration_ms=${event.durationMillis} " +
                "duration_us=${event.durationMicros} " +
                "call_count=${event.callCount ?: -1} result_count=${event.resultCount ?: -1} " +
                "response_bytes=${event.responseBytes ?: -1} outcome=${event.outcome}"
        )
    }
}

internal class MessagesMeasurement<T>(
    val count: (T) -> Int? = { null },
    val callCount: (T) -> Int? = { 1 },
    val responseBytes: (T) -> Int? = { null }
)

internal suspend fun <T> measuredMessagesOperation(
    instrumentation: MessagesInstrumentation,
    operation: String,
    count: (T) -> Int?,
    block: suspend () -> T
): T = measuredMessagesOperation(instrumentation, operation, MessagesMeasurement(count = count), block)

@Suppress("TooGenericExceptionCaught")
internal suspend fun <T> measuredMessagesOperation(
    instrumentation: MessagesInstrumentation,
    operation: String,
    measurement: MessagesMeasurement<T> = MessagesMeasurement(),
    block: suspend () -> T
): T {
    val started = TimeSource.Monotonic.markNow()
    var outcome = "success"
    var resultCount: Int? = null
    var operationCallCount: Int? = null
    var byteCount: Int? = null
    var durationMicros: Long? = null
    try {
        return block().also { result ->
            durationMicros = started.elapsedNow().inWholeMicroseconds
            resultCount = measurement.count(result)
            operationCallCount = measurement.callCount(result)
            byteCount = measurement.responseBytes(result)
            outcome = (result as? Result.Error)?.let { failureOutcome(it.exception) } ?: "success"
        }
    } catch (exception: Exception) {
        outcome = failureOutcome(exception)
        throw exception
    } finally {
        runCatching {
            val elapsedMicros = durationMicros ?: started.elapsedNow().inWholeMicroseconds
            instrumentation.record(
                MessagesTimingEvent(
                    operation,
                    elapsedMicros / 1_000,
                    operationCallCount,
                    resultCount,
                    outcome,
                    byteCount,
                    elapsedMicros
                )
            )
        }
    }
}

private fun failureOutcome(exception: Throwable): String = when (exception) {
    is kotlinx.coroutines.CancellationException -> "cancelled"
    is ConversationAccessException -> "access_denied"
    else -> "failure"
}
