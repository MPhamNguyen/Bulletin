package com.jdrms.bulletin.domain.home.presentation

import androidx.activity.ComponentActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jdrms.bulletin.core.designsystem.theme.BulletinFeedCardDefaults
import com.jdrms.bulletin.core.designsystem.theme.BulletinTheme
import com.jdrms.bulletin.domain.home.domain.model.HomeActionContext
import com.jdrms.bulletin.domain.home.domain.model.availableActions
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalTestApi::class)
class HomeActionRowDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val harness = HomePlatformDeviceHarness()

    @Test fun saveButtonAndSwipeUseSameCommandAndCompleteOneExitEach() {
        harness.show(compose)
        compose.onNodeWithText("Save", substring = false).performClick()
        compose.waitUntil(5000) { harness.exits.size == 1 }
        compose.waitForIdle()
        swipe(right = true)
        compose.runOnIdle {
            assertEquals(listOf("save", "save"), harness.commands)
            assertEquals(2, harness.exits.size)
        }
    }

    @Test fun passButtonAndSwipeUseSameCommandAndCompleteOneExitEach() {
        harness.show(compose)
        compose.onNodeWithText("Pass", substring = false).performClick()
        compose.waitUntil(5000) { harness.exits.size == 1 }
        compose.waitForIdle()
        swipe(right = false)
        compose.runOnIdle {
            assertEquals(listOf("pass", "pass"), harness.commands)
            assertEquals(2, harness.exits.size)
        }
    }

    @Test fun messageButtonOpensTrayAndHidesRowFromSemantics() {
        harness.show(compose)
        compose.onNodeWithText("Message", substring = false).performClick()
        compose.onNodeWithTag(HOME_COMPOSER_FOCUS_TAG).assertIsFocused()
        compose.onNodeWithText("Pass", substring = false).assertDoesNotExist()
        compose.onNodeWithContentDescription(
            "Close message tray"
        ).performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.waitUntil(5000) { harness.ports.home.uiState.value.interaction.card == CardState.Idle }
        compose.waitForIdle()
        compose.onNodeWithText("Pass", substring = false).assertIsEnabled()
    }

    @Test fun composerCloseRespondsToTouch() {
        harness.show(compose)
        compose.runOnIdle { harness.actions.openTray() }
        val close = compose.onNodeWithContentDescription("Close message tray").assertIsDisplayed()
        close.performClick()
        compose.waitUntil(5000) { harness.ports.home.uiState.value.interaction.card == CardState.Idle }
        compose.runOnIdle { assertEquals(listOf("open", "close"), harness.commands) }
    }

    @Test fun missingCapabilitiesKeepDisabledButtonsVisible() {
        harness.show(compose, capabilities = false)
        compose.onNodeWithText("Pass", substring = false).assertIsEnabled()
        compose.onNodeWithText("Save", substring = false).assertIsDisplayed().assertIsNotEnabled()
        compose.onNodeWithText("Message", substring = false).assertIsDisplayed().assertIsNotEnabled()
    }

    @Test fun rowRemainsStationaryAndDisabledDuringDeparture() {
        harness.scale = 1f
        harness.show(compose)
        val button = compose.onNodeWithText("Pass", substring = false)
        val before = button.fetchSemanticsNode().boundsInRoot
        compose.mainClock.autoAdvance = false
        button.performClick()
        compose.mainClock.advanceTimeBy(100)
        button.assertIsDisplayed().assertIsNotEnabled()
        assertEquals(before, button.fetchSemanticsNode().boundsInRoot)
        compose.mainClock.autoAdvance = true
    }

    @Test fun keyboardFocusSurvivesPassDeparture() {
        harness.scale = 1f
        harness.show(compose)
        val button = compose.onNodeWithText("Pass", substring = false)
        compose.runOnIdle { harness.inputMode?.requestInputMode(InputMode.Keyboard) }
        button.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        button.assertIsFocused()
        button.performKeyInput { pressKey(Key.Enter) }
        compose.waitUntil(5000) { harness.exits.size == 1 }
        compose.waitForIdle()
        button.assertIsFocused()
    }

    @Test fun closingTrayReturnsKeyboardFocusToMessageButton() {
        harness.show(compose)
        val button = compose.onNodeWithText("Message", substring = false)
        compose.runOnIdle { harness.inputMode?.requestInputMode(InputMode.Keyboard) }
        button.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        button.assertIsFocused()
        button.performKeyInput { pressKey(Key.Enter) }
        compose.waitUntil(5000) { harness.ports.home.uiState.value.interaction.card == CardState.Open }
        compose.waitForIdle()
        compose.onNodeWithTag(HOME_COMPOSER_FOCUS_TAG).assertIsFocused()
        compose.onNodeWithContentDescription(
            "Close message tray"
        ).performSemanticsAction(SemanticsActions.OnClick) { it() }
        compose.waitUntil(5000) { harness.ports.home.uiState.value.interaction.card == CardState.Idle }
        compose.waitForIdle()
        button.assertIsFocused()
    }

    @Test fun largeFontLightButtonsRemainVisibleAndAtLeastFortyEightDp() = largeFontRow(false)

    @Test fun largeFontDarkButtonsRemainVisibleAndAtLeastFortyEightDp() = largeFontRow(true)

    private fun largeFontRow(dark: Boolean) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, 2f)) {
                BulletinTheme(darkTheme = dark) {
                    Box(Modifier.width(366.dp).background(BulletinFeedCardDefaults.scrim)) {
                        HomeCardActionRow(
                            CardState.Idle.availableActions(HomeActionContext(true, true, true, true))
                                .toButtonUi(),
                            {},
                            0f,
                            false
                        )
                    }
                }
            }
        }
        listOf("Pass", "Message", "Save").forEach { label ->
            val button = compose.onNodeWithText(label, substring = false).assertIsDisplayed().assertIsEnabled()
            button.assertHeightIsAtLeast(48.dp)
        }
    }

    private fun swipe(right: Boolean) {
        compose.onRoot().performTouchInput {
            swipe(Offset(width * .5f, height * .25f), Offset(width * (if (right) .95f else .05f), height * .25f))
        }
    }
}
