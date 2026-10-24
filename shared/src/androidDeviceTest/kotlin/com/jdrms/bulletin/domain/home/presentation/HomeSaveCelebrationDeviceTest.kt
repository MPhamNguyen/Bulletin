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
    fun late_success_celebrates_once_after_deck_has_advanced() {
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
    fun failed_save_never_celebrates() {
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
    fun zero_scale_completes_save_and_consumes_confirmation_without_animation() {
        var celebrations = 0
        fixture.show(compose, anchor = anchor, onCelebrate = { celebrations++ })
        swipeRight()
        compose.runOnIdle {
            assertEquals(1, fixture.home.uiState.value.currentIndex)
            assertEquals(0, celebrations)
            assertTrue(fixture.home.uiState.value.interaction.pendingEffects.isEmpty())
        }
    }

    private fun swipeRight() {
        compose.onRoot().performTouchInput {
            swipe(Offset(width * 0.5f, height * 0.25f), Offset(width * 0.95f, height * 0.25f))
        }
    }
}
