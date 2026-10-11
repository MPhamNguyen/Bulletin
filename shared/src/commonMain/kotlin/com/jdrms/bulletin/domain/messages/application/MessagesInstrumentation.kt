package com.jdrms.bulletin.domain.messages.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.domain.model.ConversationAccessException
import kotlin.time.TimeSource

data class MessagesTimingEvent(
    val operation: String,
    val durationMillis: Long,
    val callCount: Int?,
    val resultCount: Int?,
    val outcome: String
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
                "call_count=${event.callCount ?: -1} result_count=${event.resultCount ?: -1} " +
                "outcome=${event.outcome}"
        )
    }
}

@Suppress("TooGenericExceptionCaught")
internal suspend fun <T> measuredMessagesOperation(
    instrumentation: MessagesInstrumentation,
    operation: String,
    count: (T) -> Int? = { null },
    callCount: (T) -> Int? = { 1 },
    block: suspend () -> T
): T {
    val started = TimeSource.Monotonic.markNow()
    var outcome = "success"
    var resultCount: Int? = null
    var operationCallCount: Int? = null
    try {
        return block().also { result ->
            resultCount = count(result)
            operationCallCount = callCount(result)
            if (result is Result.Error) {
                outcome = if (result.exception is ConversationAccessException) "access_denied" else "failure"
            }
        }
    } catch (exception: Exception) {
        outcome = when (exception) {
            is kotlinx.coroutines.CancellationException -> "cancelled"
            is ConversationAccessException -> "access_denied"
            else -> "failure"
        }
        throw exception
    } finally {
        runCatching {
            instrumentation.record(
                MessagesTimingEvent(
                    operation,
                    started.elapsedNow().inWholeMilliseconds,
                    operationCallCount,
                    resultCount,
                    outcome
                )
            )
        }
    }
}
