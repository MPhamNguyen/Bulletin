package com.jdrms.bulletin.core.common

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class FlowUserMessengerTest {
    @Test
    fun showDeliversAOneShotMessageToTheActiveCollector() = runTest {
        val messenger = FlowUserMessenger()
        val received = backgroundScope.async { messenger.messages.first() }
        runCurrent()
        messenger.show("Profile updated")
        assertEquals("Profile updated", received.await())
    }
}
