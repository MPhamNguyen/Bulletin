package com.jdrms.bulletin.domain.home.presentation

import androidx.activity.BackEventCompat
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jdrms.bulletin.core.common.Result
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class HomeTrayDismissalDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val harness = HomePlatformDeviceHarness()
    private var fallbacks = 0

    @Test fun backClosesOpenTrayByCloseCommandOnly() {
        show()
        open()
        back()
        compose.runOnIdle {
            assertEquals(listOf("open", "close"), harness.commands)
            assertEquals(CardState.Idle, harness.ports.home.uiState.value.interaction.card)
            assertEquals(0, fallbacks)
        }
    }

    @Test fun backWhenClosedDelegatesToExistingHandler() {
        show()
        back()
        compose.runOnIdle {
            assertEquals(1, fallbacks)
            assertEquals(emptyList(), harness.commands)
        }
    }

    @Test fun escapeClosesOpenTrayFromFocusedComposer() {
        show()
        open()
        compose.onNodeWithTag(HOME_COMPOSER_FOCUS_TAG).performKeyInput { pressKey(Key.Escape) }
        compose.runOnIdle { assertEquals(listOf("open", "close"), harness.commands) }
    }

    @Test fun escapeWhenClosedDoesNotIssueACommand() {
        show()
        compose.onNodeWithTag(HOME_CARD_FOCUS_TAG).performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        compose.onNodeWithTag(HOME_CARD_FOCUS_TAG).performKeyInput { pressKey(Key.Escape) }
        compose.runOnIdle { assertEquals(emptyList(), harness.commands) }
    }

    @Test fun sendingDelegatesBackAndIgnoresEscapeWithoutLosingOrDuplicatingMessage() {
        val sent = CompletableDeferred<Result<Unit>>()
        harness.ports.sendResult = { sent.await() }
        show()
        open()
        compose.runOnIdle {
            harness.actions.onDraftChanged("hello")
            harness.actions.sendMessage()
        }
        compose.waitForIdle()
        back()
        compose.onRoot().performKeyInput { pressKey(Key.Escape) }
        compose.runOnIdle {
            assertEquals(CardState.Sending, harness.ports.home.uiState.value.interaction.card)
            assertEquals(listOf("open"), harness.commands)
            assertEquals(1, fallbacks)
            sent.complete(Result.Success(Unit))
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(1, harness.ports.sends.size)
            assertEquals(1, harness.receipts.size)
            assertEquals(1, harness.exits.size)
        }
    }

    @Test fun leavingDelegatesBackAndIgnoresEscape() {
        harness.scale = 1f
        show()
        compose.mainClock.autoAdvance = false
        compose.runOnIdle { harness.actions.passCurrentCard() }
        compose.mainClock.advanceTimeBy(32)
        back()
        compose.onRoot().performKeyInput { pressKey(Key.Escape) }
        compose.runOnIdle {
            assertEquals(CardState.Leaving, harness.ports.home.uiState.value.interaction.card)
            assertEquals(listOf("pass"), harness.commands)
            assertEquals(1, fallbacks)
        }
        compose.mainClock.autoAdvance = true
    }

    @Test fun cancelledPredictiveBackKeepsTrayOpenAndCommitClosesOnce() {
        show()
        open()
        compose.runOnIdle {
            val dispatcher = compose.activity.onBackPressedDispatcher
            dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 0f, 0f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackProgressed(BackEventCompat(20f, 0f, .5f, BackEventCompat.EDGE_LEFT))
            dispatcher.dispatchOnBackCancelled()
            assertEquals(CardState.Open, harness.ports.home.uiState.value.interaction.card)
        }
        back()
        compose.runOnIdle { assertEquals(listOf("open", "close"), harness.commands) }
    }

    private fun show() {
        compose.runOnUiThread {
            compose.activity.onBackPressedDispatcher.addCallback(
                compose.activity,
                object : OnBackPressedCallback(true) {
                    override fun handleOnBackPressed() { fallbacks++ }
                }
            )
        }
        harness.show(compose)
    }

    private fun open() {
        compose.runOnIdle { harness.actions.openTray() }
        compose.waitForIdle()
    }

    private fun back() {
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }
}
