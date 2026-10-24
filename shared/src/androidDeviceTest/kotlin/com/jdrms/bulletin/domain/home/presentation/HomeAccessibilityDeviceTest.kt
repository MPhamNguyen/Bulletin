package com.jdrms.bulletin.domain.home.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
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
@OptIn(ExperimentalTestApi::class)
class HomeAccessibilityDeviceTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    private val harness = HomePlatformDeviceHarness()

    @Test fun saveActionAndRightSwipeCallSameCommand() {
        harness.show(compose)
        action("Save")
        swipe(right = true)
        compose.runOnIdle { assertEquals(listOf("save", "save"), harness.commands) }
    }

    @Test fun passActionAndLeftSwipeCallSameCommand() {
        harness.show(compose)
        action("Pass")
        swipe(right = false)
        compose.runOnIdle { assertEquals(listOf("pass", "pass"), harness.commands) }
    }

    @Test fun messageActionAndUpwardDragCallSameCommandAndFocusComposer() {
        harness.show(compose)
        action("Message seller")
        compose.onNodeWithTag(HOME_COMPOSER_FOCUS_TAG).assertIsFocused()
        compose.runOnIdle { harness.actions.closeTray() }
        compose.onNodeWithTag(HOME_CARD_FOCUS_TAG).assertIsFocused()
        compose.onRoot().performTouchInput {
            swipe(Offset(width * .5f, height * .5f), Offset(width * .5f, height * .2f), 500)
        }
        compose.runOnIdle { assertEquals(listOf("open", "close", "open"), harness.commands) }
    }

    @Test fun unavailableSaveAndMessageActionsAreAbsentAndArrowsDoNothing() {
        harness.show(compose, capabilities = false)
        val card = compose.onNodeWithTag(HOME_CARD_FOCUS_TAG)
        assertEquals(listOf("Pass"), card.fetchSemanticsNode().config[SemanticsActions.CustomActions].map { it.label })
        card.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        card.performKeyInput {
            pressKey(Key.DirectionRight)
            pressKey(Key.DirectionUp)
        }
        compose.runOnIdle { assertTrue(harness.commands.isEmpty()) }
    }

    @Test fun actionsAreAbsentOutsideIdleAndClosedTrayHasNoComposer() {
        harness.show(compose)
        compose.onNodeWithTag(HOME_COMPOSER_FOCUS_TAG).assertDoesNotExist()
        action("Message seller")
        assertTrue(
            compose.onNodeWithTag(HOME_CARD_FOCUS_TAG).fetchSemanticsNode()
                .config[SemanticsActions.CustomActions].isEmpty()
        )
    }

    @Test fun focusedCardArrowsMapToTheSameCommands() {
        harness.show(compose)
        val card = compose.onNodeWithTag(HOME_CARD_FOCUS_TAG)
        card.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        card.performKeyInput { pressKey(Key.DirectionRight) }
        card.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        card.performKeyInput { pressKey(Key.DirectionLeft) }
        card.performSemanticsAction(SemanticsActions.RequestFocus) { it() }
        card.performKeyInput { pressKey(Key.DirectionUp) }
        compose.runOnIdle { assertEquals(listOf("save", "pass", "open"), harness.commands) }
    }

    @Test fun descriptionContainsTitlePriceAndOnlyAvailableActions() {
        harness.show(compose, capabilities = false)
        val description = compose.onNodeWithTag(HOME_CARD_FOCUS_TAG).fetchSemanticsNode()
            .config[SemanticsProperties.ContentDescription].single()
        assertTrue(description.contains("Listing first"))
        assertTrue(description.contains("10.00"))
        assertTrue(description.contains("Pass"))
        assertTrue(!description.contains("Save"))
    }

    private fun action(label: String) {
        compose.onNodeWithTag(HOME_CARD_FOCUS_TAG).performCustomAccessibilityActionWithLabel(label)
        compose.waitForIdle()
    }

    private fun swipe(right: Boolean) {
        compose.onRoot().performTouchInput {
            swipe(Offset(width * .5f, height * .25f), Offset(width * (if (right) .95f else .05f), height * .25f))
        }
    }
}
