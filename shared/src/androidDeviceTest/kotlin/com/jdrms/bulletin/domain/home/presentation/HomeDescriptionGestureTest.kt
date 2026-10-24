package com.jdrms.bulletin.domain.home.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class HomeDescriptionGestureTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val fixture = HomeTrayDeviceFixture()
    private val description = "Description with more detail about the listing and campus pickup. ".repeat(150)

    @Test
    fun collapsed_description_upward_drag_opens_tray() {
        fixture.show(compose, description)
        dragDescription()
        compose.runOnIdle { assertEquals(CardState.Open, fixture.home.uiState.value.interaction.card) }
    }

    @Test
    fun expanded_overflow_scrolls_and_keeps_tray_closed_including_fling() {
        showExpanded()
        val before = scrollValue()
        dragDescription(100)
        compose.runOnIdle { assertEquals(CardState.Idle, fixture.home.uiState.value.interaction.card) }
        assertTrue(scrollValue() > before)
    }

    @Test
    fun new_upward_drag_at_end_opens_tray() {
        showExpanded()
        repeat(40) {
            val range = scrollRange()
            if (range.value() < range.maxValue()) dragDescription(100)
        }
        val range = scrollRange()
        assertEquals(range.maxValue(), range.value())
        compose.runOnIdle { assertEquals(CardState.Idle, fixture.home.uiState.value.interaction.card) }
        dragDescription()
        compose.runOnIdle { assertEquals(CardState.Open, fixture.home.uiState.value.interaction.card) }
    }

    @Test
    fun horizontal_swipe_over_description_still_passes() {
        showExpanded()
        compose.onNodeWithTag("home-description-first").performTouchInput {
            swipe(Offset(width * 0.8f, height * 0.75f), Offset(width * 0.05f, height * 0.75f), 400)
        }
        compose.runOnIdle { assertEquals(1, fixture.home.uiState.value.currentIndex) }
    }

    private fun showExpanded() {
        fixture.show(compose, description)
        compose.onNodeWithText(description).performClick()
        compose.waitForIdle()
        assertTrue(scrollRange().maxValue() > 0f, compose.onRoot().printToString())
    }

    private fun scrollRange() = compose.onNodeWithTag("home-description-first").fetchSemanticsNode()
        .config[SemanticsProperties.VerticalScrollAxisRange]

    private fun scrollValue() = scrollRange().value()

    private fun dragDescription(duration: Long = 500) {
        compose.onNodeWithTag("home-description-first").performTouchInput {
            swipe(Offset(width * 0.5f, height * 0.85f), Offset(width * 0.5f, height * 0.2f), duration)
        }
    }
}
