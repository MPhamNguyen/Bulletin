package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.application.IncomingMessageAlert
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class InboxNotificationCoordinatorTest {
    private val inbox = MutableStateFlow(bannerInbox("m1", at = 1_000))
    private val tracker = ActiveConversationTracker()
    private val notifier = RecordingNotifier()

    private fun kotlinx.coroutines.test.TestScope.coordinator() =
        InboxNotificationCoordinator(inbox, tracker, notifier, backgroundScope)

    @Test
    fun `foreground alert goes to the banner flow and not the system notifier`() = runTest(UnconfinedTestDispatcher()) {
        tracker.setAppForeground(true)
        val coordinator = coordinator()
        val banners = mutableListOf<IncomingMessageAlert>()
        backgroundScope.launch { coordinator.alerts.collect { banners += it } }
        coordinator.start()
        inbox.value = bannerInbox("m2", at = 2_000)
        assertEquals(listOf("m2"), banners.map { it.messageId.value })
        assertTrue(notifier.shown.isEmpty())
    }

    @Test
    fun `background alert goes to the system notifier and not the banner flow`() = runTest(UnconfinedTestDispatcher()) {
        tracker.setAppForeground(false)
        val coordinator = coordinator()
        val banners = mutableListOf<IncomingMessageAlert>()
        backgroundScope.launch { coordinator.alerts.collect { banners += it } }
        coordinator.start()
        inbox.value = bannerInbox("m2", at = 2_000)
        assertEquals(listOf("m2"), notifier.shown.map { it.messageId.value })
        assertTrue(banners.isEmpty())
    }

    @Test
    fun `routing follows the foreground signal at the time of the alert`() = runTest(UnconfinedTestDispatcher()) {
        val coordinator = coordinator()
        val banners = mutableListOf<IncomingMessageAlert>()
        backgroundScope.launch { coordinator.alerts.collect { banners += it } }
        coordinator.start()
        tracker.setAppForeground(true)
        inbox.value = bannerInbox("m2", at = 2_000)
        tracker.setAppForeground(false)
        inbox.value = bannerInbox("m3", at = 3_000)
        assertEquals(listOf("m2"), banners.map { it.messageId.value })
        assertEquals(listOf("m3"), notifier.shown.map { it.messageId.value })
    }

    @Test
    fun `alerts are not replayed to a late collector`() = runTest(UnconfinedTestDispatcher()) {
        tracker.setAppForeground(true)
        val coordinator = coordinator()
        coordinator.start()
        inbox.value = bannerInbox("m2", at = 2_000)
        val late = mutableListOf<IncomingMessageAlert>()
        backgroundScope.launch { coordinator.alerts.collect { late += it } }
        assertTrue(late.isEmpty())
    }
}
