package com.jdrms.bulletin.domain.home.presentation

import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class HomeDragRestorationDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    @Test fun activity_recreation_during_drag_cancels_without_committing() {
        val fixture = HomeTrayDeviceFixture()
        fixture.show(compose)
        compose.onNodeWithTag(HOME_CARD_FOCUS_TAG).performTouchInput {
            down(Offset(width * .5f, height * .25f))
            moveBy(Offset(50f, 0f))
        }
        compose.runOnIdle { assertEquals(CardState.Swiping, fixture.home.uiState.value.interaction.card) }
        compose.activityRule.scenario.recreate()
        compose.activityRule.scenario.onActivity { it.setContent(content = fixture.content) }
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(CardState.Idle, fixture.home.uiState.value.interaction.card)
            assertEquals(0, fixture.home.uiState.value.currentIndex)
            assertTrue(fixture.saves.isEmpty())
            assertTrue(fixture.home.uiState.value.interaction.pendingEffects.isEmpty())
        }
        compose.onNodeWithTag(HOME_CARD_FOCUS_TAG).performTouchInput {
            swipe(Offset(width * .8f, height * .25f), Offset(width * .05f, height * .25f), 400)
        }
        compose.runOnIdle { assertEquals(1, fixture.home.uiState.value.currentIndex) }
    }

    @Test fun saved_state_recreation_during_drag_cancels_without_committing_and_accepts_next_swipe() {
        val fixture = HomeTrayDeviceFixture()
        val restoration = StateRestorationTester(compose)
        fixture.show(compose, restoration = restoration)
        val card = compose.onNodeWithTag(HOME_CARD_FOCUS_TAG)
        card.performTouchInput {
            down(Offset(width * .5f, height * .25f))
            moveBy(Offset(50f, 0f))
        }
        compose.runOnIdle { assertEquals(CardState.Swiping, fixture.home.uiState.value.interaction.card) }
        restoration.emulateSavedInstanceStateRestore()
        compose.waitForIdle()
        compose.runOnIdle {
            assertEquals(CardState.Idle, fixture.home.uiState.value.interaction.card)
            assertEquals(0, fixture.home.uiState.value.currentIndex)
            assertTrue(fixture.saves.isEmpty())
            assertTrue(fixture.home.uiState.value.interaction.pendingEffects.isEmpty())
        }
        card.performTouchInput {
            cancel()
            swipe(Offset(width * .8f, height * .25f), Offset(width * .05f, height * .25f), 400)
        }
        compose.runOnIdle { assertEquals(1, fixture.home.uiState.value.currentIndex) }
    }
}
