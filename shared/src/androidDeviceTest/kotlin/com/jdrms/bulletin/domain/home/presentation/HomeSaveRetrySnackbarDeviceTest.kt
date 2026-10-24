package com.jdrms.bulletin.domain.home.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.application.SaveListingFailure
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.SwipeVerdict
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class HomeSaveRetrySnackbarDeviceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val fixture = HomeTrayDeviceFixture()

    @Test
    fun retry_after_advance_targets_original_listing_and_acknowledges_shown_effect() {
        showFailure()
        compose.runOnIdle {
            assertEquals(1, fixture.home.uiState.value.currentIndex)
            assertTrue(fixture.home.uiState.value.interaction.pendingEffects.isEmpty())
            fixture.saveResult = { Result.Success(Unit) }
        }
        compose.onNodeWithText("Retry").performClick()
        compose.runOnIdle {
            assertEquals(listOf(HomeItemId("first"), HomeItemId("first")), fixture.saves)
            assertEquals(1, fixture.home.uiState.value.currentIndex)
        }
    }

    @Test
    fun dismiss_removes_failed_save() {
        showFailure()
        compose.onNodeWithContentDescription("Dismiss").performClick()
        compose.waitForIdle()
        assertTrue(runBlocking { fixture.decisions.getExcludedListingIds("home-test", SwipeVerdict.SAVE).getOrNull()!! }.isEmpty())
    }

    @Test
    fun recreation_keeps_acknowledged_retry_available_without_dismissing_it() {
        val restoration = StateRestorationTester(compose)
        fixture.saveResult = { Result.Error(SaveListingFailure.Network()) }
        fixture.showWithZeroMotion(compose, restoration = restoration)
        swipeRight()
        compose.onNodeWithText("Retry").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Retry").assertExists()
        assertEquals(setOf(HomeItemId("first")), runBlocking {
            fixture.decisions.getExcludedListingIds("home-test", SwipeVerdict.SAVE).getOrNull()
        })
        compose.runOnIdle { fixture.saveResult = { Result.Success(Unit) } }
        compose.onNodeWithText("Retry").performClick()
        compose.runOnIdle { assertEquals(2, fixture.saves.size) }
    }

    @Test
    fun failure_snackbar_is_polite_and_retry_is_reachable() {
        showFailure()
        val polite = SemanticsMatcher.expectValue(SemanticsProperties.LiveRegion, LiveRegionMode.Polite)
        compose.onNode(hasText("Could not save listing.") and hasAnyAncestor(polite), useUnmergedTree = true)
            .assertExists()
        compose.onNodeWithText("Retry").assertHasClickAction().assertIsEnabled()
    }

    private fun showFailure() {
        fixture.saveResult = { Result.Error(SaveListingFailure.Network()) }
        fixture.showWithZeroMotion(compose)
        swipeRight()
        compose.onNodeWithText("Retry").assertExists()
    }

    private fun swipeRight() {
        compose.onRoot().performTouchInput {
            swipe(Offset(width * 0.5f, height * 0.25f), Offset(width * 0.95f, height * 0.25f))
        }
    }
}
