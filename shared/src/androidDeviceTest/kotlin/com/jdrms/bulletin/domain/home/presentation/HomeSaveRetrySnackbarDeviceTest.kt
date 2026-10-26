package com.jdrms.bulletin.domain.home.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.SemanticsProperties
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
    fun retryAfterAdvanceTargetsOriginalListingAndAcknowledgesShownEffect() {
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
    fun dismissRemovesFailedSave() {
        showFailure()
        compose.onNodeWithContentDescription("Dismiss").performClick()
        compose.waitForIdle()
        val excluded = runBlocking {
            fixture.decisions.getExcludedListingIds("home-test", SwipeVerdict.SAVE).getOrNull()!!
        }
        assertTrue(excluded.isEmpty())
    }

    @Test
    fun recreationKeepsAcknowledgedRetryAvailableWithoutDismissingIt() {
        val restoration = StateRestorationTester(compose)
        fixture.saveResult = { Result.Error(SaveListingFailure.Network()) }
        fixture.showWithZeroMotion(compose, restoration = restoration)
        swipeRight()
        compose.onNodeWithText("Retry").assertExists()
        restoration.emulateSavedInstanceStateRestore()
        compose.onNodeWithText("Retry").assertExists()
        assertEquals(
            setOf(HomeItemId("first")),
            runBlocking {
                fixture.decisions.getExcludedListingIds("home-test", SwipeVerdict.SAVE).getOrNull()
            }
        )
        compose.runOnIdle { fixture.saveResult = { Result.Success(Unit) } }
        compose.onNodeWithText("Retry").performClick()
        compose.runOnIdle { assertEquals(2, fixture.saves.size) }
    }

    @Test
    fun failureSnackbarIsPoliteAndRetryIsReachable() {
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
