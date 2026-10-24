package com.jdrms.bulletin.domain.home.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jdrms.bulletin.app.ProfileTabCelebration
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Real specs and VM acknowledgements at scale 1; numerical endpoint coverage lives in commonTest. */
@RunWith(AndroidJUnit4::class)
class HomeNonzeroMotionDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val harness = HomePlatformDeviceHarness()

    @Test fun saveDepartureAndRealProfileCelebrationCompleteOnce() {
        var haptics = 0
        val celebration = ProfileTabCelebration()
        harness.onCelebration = { scope, scale ->
            celebration.play(
                scope, scale,
                object : HapticFeedback {
                    override fun performHapticFeedback(hapticFeedbackType: HapticFeedbackType) { haptics++ }
                }
            )
        }
        start()
        compose.runOnIdle { harness.actions.saveCurrentCard() }
        advanceFrames()
        compose.runOnIdle {
            assertEquals(1, harness.exits.size)
            assertEquals(1, harness.consumed.size)
            assertEquals(listOf(1f), harness.celebrations)
            assertEquals(1, haptics)
            assertEquals(1, harness.ports.home.uiState.value.currentIndex)
        }
        advanceFrames()
        compose.runOnIdle {
            assertEquals(1, harness.exits.size)
            assertEquals(1, haptics)
        }
    }

    @Test fun passDepartureCompletesOnce() {
        start()
        compose.runOnIdle { harness.actions.passCurrentCard() }
        advanceFrames()
        compose.runOnIdle {
            assertEquals(1, harness.exits.size)
            assertEquals(1, harness.ports.home.uiState.value.currentIndex)
            assertTrue(harness.celebrations.isEmpty())
        }
        advanceFrames()
        compose.runOnIdle { assertEquals(1, harness.exits.size) }
    }

    @Test fun sendReceiptAndDepartureCompleteOnce() {
        start()
        compose.runOnIdle {
            harness.actions.openTray()
            harness.actions.onDraftChanged("hello")
        }
        advanceFrames()
        compose.runOnIdle { harness.actions.sendMessage() }
        advanceFrames()
        compose.runOnIdle {
            assertEquals(1, harness.receipts.size)
            assertEquals(1, harness.exits.size)
            assertEquals(1, harness.ports.sends.size)
            assertEquals(1, harness.ports.home.uiState.value.currentIndex)
        }
        advanceFrames()
        compose.runOnIdle {
            assertEquals(1, harness.exits.size)
            assertEquals(1, harness.receipts.size)
        }
    }

    private fun start() {
        harness.scale = 1f
        harness.show(compose)
        compose.mainClock.autoAdvance = false
    }

    private fun advanceFrames() {
        repeat(200) {
            compose.mainClock.advanceTimeByFrame()
            compose.runOnIdle { }
        }
    }
}
