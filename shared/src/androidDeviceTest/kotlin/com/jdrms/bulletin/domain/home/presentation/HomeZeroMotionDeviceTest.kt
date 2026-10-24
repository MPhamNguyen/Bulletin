package com.jdrms.bulletin.domain.home.presentation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jdrms.bulletin.app.ProfileTabCelebration
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.application.SaveListingFailure
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class HomeZeroMotionDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val harness = HomePlatformDeviceHarness()

    @Test fun zero_scale_save_calls_exit_consumption_and_celebration_exactly_once() {
        harness.show(compose)
        compose.runOnIdle { harness.actions.saveCurrentCard() }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1, harness.exits.size)
            assertEquals(1, harness.consumed.size)
            assertEquals(listOf(0f), harness.celebrations)
            assertEquals(1, harness.ports.home.uiState.value.currentIndex)
            harness.ports.home.onExitCompleted(harness.exits.single())
            harness.ports.home.onEffectConsumed(harness.consumed.single())
            harness.replayedConfirmation = HomeEffect.SaveConfirmed(harness.consumed.single(), HomeItemId("first"))
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1, harness.exits.size)
            assertEquals(1, harness.consumed.size)
            assertEquals(listOf(0f), harness.celebrations)
        }
    }

    @Test fun zero_scale_pass_calls_exit_once_without_celebration() {
        harness.show(compose)
        compose.runOnIdle { harness.actions.passCurrentCard() }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1, harness.exits.size)
            assertEquals(1, harness.ports.home.uiState.value.currentIndex)
            assertTrue(harness.celebrations.isEmpty())
            assertTrue(harness.consumed.isEmpty())
        }
    }

    @Test fun zero_scale_send_calls_receipt_and_exit_once_without_celebration() {
        harness.show(compose)
        compose.runOnIdle { harness.actions.openTray(); harness.actions.onDraftChanged("hello") }
        compose.waitForIdle()
        compose.runOnIdle { harness.actions.sendMessage() }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1, harness.receipts.size)
            assertEquals(1, harness.exits.size)
            assertEquals(1, harness.ports.sends.size)
            assertEquals(1, harness.ports.home.uiState.value.currentIndex)
            harness.ports.home.onSentReceiptCompleted(harness.receipts.single())
            harness.ports.home.onExitCompleted(harness.exits.single())
            assertTrue(harness.celebrations.isEmpty())
        }
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, harness.exits.size); assertEquals(1, harness.receipts.size) }
    }

    @Test fun retry_success_celebrates_without_another_departure() {
        harness.ports.saveResult = { Result.Error(SaveListingFailure.Network()) }
        harness.show(compose)
        compose.runOnIdle { harness.actions.saveCurrentCard() }
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(harness.celebrations.isEmpty())
            harness.ports.saveResult = { Result.Success(Unit) }
            harness.actions.retrySave(HomeItemId("first"))
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(listOf(0f), harness.celebrations)
            assertEquals(1, harness.exits.size)
            assertEquals(1, harness.ports.home.uiState.value.currentIndex)
        }
    }

    @Test fun unavailable_save_never_delivers_a_celebration_or_exit() {
        harness.show(compose, capabilities = false)
        compose.runOnIdle { harness.actions.saveCurrentCard() }
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(harness.celebrations.isEmpty())
            assertTrue(harness.exits.isEmpty())
        }
    }

    @Test fun changing_scale_mid_departure_does_not_duplicate_exit() {
        harness.scale = 2f
        harness.show(compose)
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { harness.actions.passCurrentCard() }
        compose.mainClock.advanceTimeBy(32)
        compose.runOnIdle { harness.scale = 0f }
        compose.mainClock.advanceTimeBy(64)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.runOnIdle { assertEquals(1, harness.exits.size) }
    }

    @Test fun zero_scale_profile_ripple_and_pulse_finish_at_rest_and_keep_haptic() {
        var haptics = 0
        compose.setContent {
            val scope = rememberCoroutineScope()
            val celebration = remember { ProfileTabCelebration() }
            LaunchedEffect(Unit) {
                celebration.play(scope, 0f, object : HapticFeedback {
                    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) { haptics++ }
                })
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Box(Modifier.size(48.dp).background(Color.White).then(celebration.modifier(Color.Red)).testTag("pulse"))
            }
        }
        compose.waitForIdle()
        assertEquals(1, haptics)
        compose.onNodeWithTag("pulse").assertWidthIsEqualTo(48.dp).assertHeightIsEqualTo(48.dp)
        val pixels = compose.onNodeWithTag("pulse").captureToImage().toPixelMap()
        assertEquals(Color.White, pixels[pixels.width / 2, pixels.height / 2])
    }
}
