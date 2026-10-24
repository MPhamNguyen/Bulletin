package com.jdrms.bulletin.domain.home.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.application.SaveListingFailure
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class HomeSaveCelebrationDeviceTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val fixture = HomeTrayDeviceFixture()
    private val anchor = Rect(850f, 2100f, 950f, 2200f)

    @Test
    fun lateSuccessCelebratesOnceAfterDeckHasAdvanced() {
        val result = CompletableDeferred<Result<Unit>>()
        fixture.saveResult = { result.await() }
        var celebrations = 0
        fixture.show(compose, durationScale = 1f, anchor = anchor, onCelebrate = { celebrations++ })
        swipeRight()
        compose.runOnIdle {
            assertEquals(1, fixture.home.uiState.value.currentIndex)
            assertEquals(0, celebrations)
            result.complete(Result.Success(Unit))
        }
        compose.waitUntil(5000) { celebrations == 1 }
        compose.runOnIdle {
            assertEquals(1, fixture.home.uiState.value.currentIndex)
            assertTrue(fixture.home.uiState.value.interaction.pendingEffects.isEmpty())
        }
    }

    @Test
    fun failedSaveNeverCelebrates() {
        fixture.saveResult = { Result.Error(SaveListingFailure.Network()) }
        var celebrations = 0
        fixture.show(compose, durationScale = 1f, anchor = anchor, onCelebrate = { celebrations++ })
        swipeRight()
        compose.runOnIdle {
            assertEquals(1, fixture.home.uiState.value.currentIndex)
            assertEquals(0, celebrations)
        }
    }

    @Test
    fun zeroScaleCompletesSaveAndDeliversCelebrationOnce() {
        var celebrations = 0
        fixture.showWithZeroMotion(compose, anchor = anchor, onCelebrate = { celebrations++ })
        swipeRight()
        compose.runOnIdle {
            assertEquals(1, fixture.home.uiState.value.currentIndex)
            assertEquals(1, celebrations)
            assertTrue(fixture.home.uiState.value.interaction.pendingEffects.isEmpty())
        }
    }

    @Test
    fun confirmedSaveWaitsForNormalDepartureThenCelebratesOnNextCardOnce() {
        val result = CompletableDeferred<Result<Unit>>()
        fixture.saveResult = { result.await() }
        var celebrations = 0
        fixture.show(compose, durationScale = 1f, anchor = anchor, onCelebrate = { celebrations++ })
        compose.mainClock.autoAdvance = false
        swipeRight()
        compose.runOnIdle {
            assertEquals(CardState.Leaving, fixture.home.uiState.value.interaction.card)
            assertEquals(0, fixture.home.uiState.value.currentIndex)
            result.complete(Result.Success(Unit))
        }
        compose.mainClock.advanceTimeBy(32)
        compose.runOnIdle {
            assertTrue(fixture.home.uiState.value.interaction.pendingEffects.any { it is HomeEffect.SaveConfirmed })
            assertEquals(0, celebrations)
        }
        compose.mainClock.advanceTimeBy(1500)
        compose.runOnIdle {
            assertEquals(1, fixture.home.uiState.value.currentIndex)
            assertEquals(CardState.Idle, fixture.home.uiState.value.interaction.card)
            assertEquals(1, celebrations)
            assertEquals(listOf(HomeItemId("first")), fixture.saves)
            assertTrue(fixture.home.uiState.value.interaction.pendingEffects.isEmpty())
        }
        compose.mainClock.advanceTimeBy(1500)
        compose.runOnIdle { assertEquals(1, celebrations) }
        compose.mainClock.autoAdvance = true
    }

    private fun swipeRight() {
        compose.onRoot().performTouchInput {
            swipe(Offset(width * 0.5f, height * 0.25f), Offset(width * 0.95f, height * 0.25f))
        }
    }
}
