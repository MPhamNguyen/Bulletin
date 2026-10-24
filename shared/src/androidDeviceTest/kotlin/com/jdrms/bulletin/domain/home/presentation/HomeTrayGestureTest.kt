package com.jdrms.bulletin.domain.home.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.application.ContactSellerFailure
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class HomeTrayGestureTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val fixture = HomeTrayDeviceFixture()

    @Test
    fun upward_drag_opens_and_downward_drag_closes_without_sending() {
        fixture.show(compose)
        dragUp()
        assertOpen()
        compose.onRoot().performTouchInput {
            swipe(Offset(width * 0.5f, height * 0.3f), Offset(width * 0.5f, height * 0.6f), 500)
        }
        compose.runOnIdle { assertEquals(CardState.Idle, fixture.home.uiState.value.interaction.card) }
        assertTrue(fixture.sends.isEmpty())
    }

    @Test
    fun fast_upward_flick_only_opens_and_quick_reply_only_fills_draft() {
        fixture.show(compose)
        dragUp(40)
        assertOpen()
        compose.onNodeWithText("Is this still available?").performClick()
        compose.runOnIdle {
            assertEquals("Is this still available?", fixture.home.uiState.value.interaction.tray.draft)
            assertTrue(fixture.sends.isEmpty())
        }
    }

    @Test
    fun button_send_failure_restores_draft_and_keeps_tray_open() {
        fixture.sendResult = { Result.Error(ContactSellerFailure.Network()) }
        fixture.show(compose)
        dragUp()
        compose.onNode(hasSetTextAction()).performTextInput("meet on campus")
        compose.onNodeWithContentDescription("Send message").performClick()
        compose.onNodeWithText("Message not sent. Try again.").assertExists()
        compose.runOnIdle {
            assertEquals("meet on campus", fixture.home.uiState.value.interaction.tray.draft)
            assertEquals(0, fixture.home.uiState.value.currentIndex)
        }
        assertOpen()
    }

    @Test
    fun ime_send_receipt_dwell_then_departure_and_duplicate_tokens_are_harmless() {
        val result = CompletableDeferred<Result<Unit>>()
        fixture.sendResult = { result.await() }
        fixture.show(compose, durationScale = 1f)
        dragUp()
        compose.onNode(hasSetTextAction()).performTextInput("hello")
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.runOnIdle { result.complete(Result.Success(Unit)) }
        compose.waitUntil(5000) { fixture.home.uiState.value.interaction.tray.sent }
        compose.mainClock.advanceTimeBy(100)
        var token = 0L
        compose.runOnIdle {
            token = requireNotNull(fixture.home.uiState.value.interaction.tray.sentReceiptId)
            assertEquals(CardState.Sending, fixture.home.uiState.value.interaction.card)
        }
        compose.onNodeWithText("Message sent").assertExists()
        compose.mainClock.advanceTimeBy(1000)
        compose.runOnIdle { assertEquals(0, fixture.home.uiState.value.currentIndex) }
        compose.mainClock.advanceTimeBy(1200)
        compose.runOnIdle {
            assertEquals(1, fixture.home.uiState.value.currentIndex)
            fixture.home.onSentReceiptCompleted(token)
            assertEquals(1, fixture.home.uiState.value.currentIndex)
            assertEquals(1, fixture.sends.size)
        }
    }

    private fun dragUp(duration: Long = 500) {
        compose.onRoot().performTouchInput {
            swipe(Offset(width * 0.5f, height * 0.5f), Offset(width * 0.5f, height * 0.2f), duration)
        }
    }

    private fun assertOpen() {
        compose.runOnIdle { assertEquals(CardState.Open, fixture.home.uiState.value.interaction.card) }
    }
}
