package com.jdrms.bulletin.domain.messages.presentation

import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.SenderId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class InAppMessageBannerControllerTest {
    private val inbox = MutableStateFlow(InboxState(bannerViewer, isLoading = false))
    private val alerts = MutableSharedFlow<com.jdrms.bulletin.domain.messages.application.IncomingMessageAlert>(
        extraBufferCapacity = 8
    )

    private fun TestScope.controller(eligible: Boolean = true) =
        InAppMessageBannerController(inbox, backgroundScope).also {
            it.start(alerts)
            it.setEligible(eligible)
        }

    @Test
    fun `eligible alert becomes visible`() = runTest(UnconfinedTestDispatcher()) {
        val controller = controller()
        alerts.emit(bannerAlert("m1"))
        assertEquals(MessageId("m1"), controller.visible.value?.messageId)
    }

    @Test
    fun `alert on an ineligible screen is dropped and not shown later`() = runTest(UnconfinedTestDispatcher()) {
        val controller = controller(eligible = false)
        alerts.emit(bannerAlert("m1"))
        controller.setEligible(true)
        assertNull(controller.visible.value)
    }

    @Test
    fun `becoming ineligible clears the visible banner and queue`() = runTest(UnconfinedTestDispatcher()) {
        val controller = controller()
        alerts.emit(bannerAlert("m1", "a"))
        alerts.emit(bannerAlert("m2", "b"))
        controller.setEligible(false)
        controller.setEligible(true)
        assertNull(controller.visible.value)
    }

    @Test
    fun `dismiss shows the next queued banner`() = runTest(UnconfinedTestDispatcher()) {
        val controller = controller()
        alerts.emit(bannerAlert("m1", "a"))
        alerts.emit(bannerAlert("m2", "b"))
        controller.dismiss(MessageId("m1"))
        assertEquals(MessageId("m2"), controller.visible.value?.messageId)
    }

    @Test
    fun `sign-out clears the banner`() = runTest(UnconfinedTestDispatcher()) {
        val controller = controller()
        alerts.emit(bannerAlert("m1"))
        inbox.value = InboxState(viewerId = null, isLoading = false)
        assertNull(controller.visible.value)
    }

    @Test
    fun `account switch clears the banner and rejects the old account's alerts`() = runTest(
        UnconfinedTestDispatcher()
    ) {
        val controller = controller()
        alerts.emit(bannerAlert("m1"))
        inbox.value = InboxState(viewerId = SenderId("someone-else"), isLoading = false)
        assertNull(controller.visible.value)
        alerts.emit(bannerAlert("m2", recipient = bannerViewer))
        assertNull(controller.visible.value)
    }
}
