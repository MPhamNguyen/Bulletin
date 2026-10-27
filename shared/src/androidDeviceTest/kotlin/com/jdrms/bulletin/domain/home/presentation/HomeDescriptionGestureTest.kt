package com.jdrms.bulletin.domain.home.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.Velocity
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
    fun collapsedDescriptionUpwardDragOpensTray() {
        fixture.showWithZeroMotion(compose, description)
        dragDescription()
        compose.runOnIdle { assertEquals(CardState.Open, fixture.home.uiState.value.interaction.card) }
    }

    @Test
    fun expandedOverflowScrollsAndKeepsTrayClosedIncludingFling() {
        showExpanded()
        val before = scrollValue()
        dragDescription(100)
        compose.runOnIdle { assertEquals(CardState.Idle, fixture.home.uiState.value.interaction.card) }
        assertTrue(scrollValue() > before)
    }

    @Test
    fun newUpwardDragAtEndOpensTray() {
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
    fun horizontalSwipeOverDescriptionStillPasses() {
        showExpanded()
        compose.onNodeWithTag("home-description-first").performTouchInput {
            swipe(Offset(width * 0.8f, height * 0.75f), Offset(width * 0.05f, height * 0.75f), 400)
        }
        compose.runOnIdle { assertEquals(1, fixture.home.uiState.value.currentIndex) }
    }

    @Test
    fun releasedDescriptionFlingContinuesWithoutOpeningTrayOrNestedParent() {
        var parentScrolls = 0
        var parentFlings = 0
        val parent = object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                parentScrolls++
                return Offset.Zero
            }
            override suspend fun onPreFling(available: Velocity): Velocity {
                parentFlings++
                return Velocity.Zero
            }
        }
        fixture.showWithZeroMotion(compose, description, modifier = Modifier.nestedScroll(parent))
        compose.onNodeWithText(description).performClick()
        compose.waitForIdle()
        compose.mainClock.autoAdvance = false
        compose.onNodeWithTag("home-description-first").performTouchInput {
            down(Offset(width * .5f, height * .85f))
            moveTo(Offset(width * .5f, height * .65f), 40)
            moveTo(Offset(width * .5f, height * .45f), 40)
        }
        val atRelease = scrollValue()
        compose.onNodeWithTag("home-description-first").performTouchInput { up() }
        compose.mainClock.advanceTimeBy(160)
        compose.waitForIdle()
        val afterRelease = scrollValue()
        assertTrue(
            afterRelease > atRelease,
            "Scroll after release: $atRelease -> $afterRelease, max=${scrollRange().maxValue()}"
        )
        compose.mainClock.advanceTimeBy(3000)
        compose.runOnIdle {
            assertEquals(CardState.Idle, fixture.home.uiState.value.interaction.card)
            assertEquals(0, fixture.home.uiState.value.currentIndex)
            assertEquals(0, parentScrolls)
            assertEquals(0, parentFlings)
            assertTrue(fixture.sends.isEmpty())
        }
        compose.mainClock.autoAdvance = true
    }

    @Test
    fun childKeepsBoundaryCrossingGestureThenNewDragOpensTray() {
        showExpanded()
        repeat(40) {
            if (scrollRange().value() < scrollRange().maxValue()) dragDescription(100)
        }
        compose.onNodeWithTag("home-description-first").performTouchInput {
            swipe(Offset(width * .5f, height * .3f), Offset(width * .5f, height * .5f), 600)
        }
        assertTrue(scrollValue() < scrollRange().maxValue())
        dragDescription(500)
        assertEquals(scrollRange().maxValue(), scrollValue())
        compose.runOnIdle { assertEquals(CardState.Idle, fixture.home.uiState.value.interaction.card) }
        dragDescription()
        compose.runOnIdle { assertEquals(CardState.Open, fixture.home.uiState.value.interaction.card) }
    }

    private fun showExpanded() {
        fixture.showWithZeroMotion(compose, description)
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
