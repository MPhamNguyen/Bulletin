package com.jdrms.bulletin.domain.home.presentation

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class HomeKeyboardDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val harness = HomePlatformDeviceHarness()

    @After fun restorePortrait() {
        compose.runOnUiThread { compose.activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT }
    }

    @Test fun portraitImeKeepsComposerAboveReservedNavbarAndPausesPhotoSeam() {
        show()
        open()
        assertComposerInViewport()
        compose.runOnIdle { assertTrue(harness.photoPaused) }
        compose.runOnIdle { harness.keyboard?.hide() }
        compose.waitUntil(5000) { !harness.photoPaused }
        assertComposerInViewport()
    }

    @Test fun landscapeImeKeepsComposerInAvailableViewport() {
        show(landscape = true)
        open()
        assertComposerInViewport()
        compose.runOnIdle { harness.keyboard?.hide() }
        compose.waitUntil(5000) { !harness.photoPaused }
        assertComposerInViewport()
    }

    @Test fun closingTrayWithImeOpenHidesKeyboardAndReturnsCardFocus() {
        show()
        open()
        compose.onNodeWithTag(HOME_COMPOSER_FOCUS_TAG).performTextInput("typing")
        compose.runOnIdle { harness.actions.closeTray() }
        compose.waitUntil(5000) { !harness.photoPaused }
        compose.onNodeWithTag(HOME_CARD_FOCUS_TAG).assertIsFocused()
        compose.onNodeWithTag(HOME_COMPOSER_FOCUS_TAG).assertDoesNotExist()
        compose.runOnIdle { assertTrue(harness.ports.sends.isEmpty()) }
    }

    @Test fun keyboardOpeningDuringDragKeepsDraftAndDoesNotSend() {
        show()
        open()
        compose.runOnIdle {
            harness.actions.onDraftChanged("keep this")
            harness.keyboard?.hide()
        }
        compose.waitUntil(5000) { !harness.photoPaused }
        val card = compose.onNodeWithTag(HOME_CARD_FOCUS_TAG)
        card.performTouchInput {
            down(Offset(width * .5f, height * .2f))
            moveBy(Offset(0f, 60f))
        }
        compose.runOnIdle { harness.keyboard?.show() }
        compose.waitUntil(5000) { harness.photoPaused }
        card.performTouchInput {
            moveBy(Offset(0f, 2f))
            up()
        }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(CardState.Open, harness.ports.home.uiState.value.interaction.card)
            assertEquals("keep this", harness.ports.home.uiState.value.interaction.tray.draft)
            assertTrue(harness.ports.sends.isEmpty())
        }
        assertComposerInViewport()
    }

    @Test fun shortViewportGivesComposerPriorityOverHeader() {
        harness.viewportHeight = 180.dp
        show()
        open()
        assertComposerInViewport()
    }

    @Test fun explicitPhotoPauseSurvivesKeyboardHiding() {
        harness.externallyPaused = true
        show()
        compose.runOnIdle { assertTrue(harness.photoPaused) }
        open()
        compose.runOnIdle { harness.actions.closeTray() }
        compose.waitForIdle()
        compose.runOnIdle {
            assertTrue(harness.photoPaused)
            harness.externallyPaused = false
        }
        compose.waitUntil(5000) { !harness.photoPaused }
    }

    private fun show(landscape: Boolean = false) {
        compose.runOnUiThread {
            compose.activity.requestedOrientation = if (landscape) {
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
        val expected = if (landscape) Configuration.ORIENTATION_LANDSCAPE else Configuration.ORIENTATION_PORTRAIT
        compose.waitUntil(5000) { compose.activity.resources.configuration.orientation == expected }
        harness.navbarSpace = 96.dp
        harness.show(compose)
    }

    private fun open() {
        compose.runOnIdle { harness.actions.openTray() }
        compose.waitUntil(5000) { harness.photoPaused }
        compose.waitForIdle()
    }

    private fun assertComposerInViewport() {
        val viewport = compose.onNodeWithTag("home_test_viewport").fetchSemanticsNode().boundsInRoot
        val composer = compose.onNodeWithTag(HOME_COMPOSER_FOCUS_TAG)
            .assertIsDisplayed()
            .fetchSemanticsNode()
            .boundsInRoot
        assertTrue(composer.top >= viewport.top, "Composer top must fit in available content")
        assertTrue(composer.bottom <= viewport.bottom + 1f, "Composer bottom must fit above IME")
        compose.onNodeWithTag(HOME_COMPOSER_FOCUS_TAG).assertHeightIsAtLeast(48.dp)
    }
}
