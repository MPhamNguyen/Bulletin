package com.jdrms.bulletin.domain.home.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SendQuickMessageCancellationTest {
    @Test
    fun cancellationWhilePortSuspendsPropagatesWithoutReturningAFailure() = runTest {
        val port = FakeContactSellerPort()
        val started = CompletableDeferred<Unit>()
        var portCancelled = false
        var returned = false
        port.respond = {
            started.complete(Unit)
            try {
                awaitCancellation()
            } finally {
                portCancelled = true
            }
        }
        val job = launch {
            SendQuickMessage(port)("viewer", HomeItemId("listing"), "hello")
            returned = true
        }

        started.await()
        job.cancelAndJoin()

        assertTrue(job.isCancelled)
        assertTrue(portCancelled)
        assertFalse(returned)
        assertEquals(1, port.calls.size)
    }

    @Test
    fun unexpectedErrorResultBecomesUnknown() = runTest {
        val port = FakeContactSellerPort()
        val cause = IllegalStateException("Unexpected adapter failure")
        port.respond = { Result.Error(cause) }

        val result = SendQuickMessage(port)("viewer", HomeItemId("listing"), "hello")

        assertSame(cause, assertIs<SendQuickMessageFailure.Unknown>(assertIs<Result.Error>(result).exception).cause)
        assertEquals(1, port.calls.size)
    }
}
