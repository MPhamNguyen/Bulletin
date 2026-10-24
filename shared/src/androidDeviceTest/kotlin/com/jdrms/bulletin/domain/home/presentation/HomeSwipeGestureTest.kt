package com.jdrms.bulletin.domain.home.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jdrms.bulletin.core.designsystem.BulletinTheme
import com.jdrms.bulletin.domain.home.application.GetPersonalizedFeed
import com.jdrms.bulletin.domain.home.application.UpdateUserPreferences
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class HomeSwipeGestureTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()
    private val saves = DeckCharacterizationSavePort()
    private lateinit var home: HomeViewModel

    @Test
    fun rightSwipeSavesOriginalListing() {
        showDeck()
        swipe(0.95f)
        compose.runOnIdle { assertEquals(listOf(HomeItemId("first")), saves.saved) }
        compose.onNodeWithText("Listing first").assertDoesNotExist()
    }

    @Test
    fun leftSwipePassesWithoutSaving() {
        showDeck()
        swipe(0.05f)
        compose.runOnIdle {
            assertEquals(emptyList(), saves.saved)
            assertEquals(1, home.uiState.value.currentIndex)
        }
    }

    @Test
    fun shortDragSnapsBackToIdle() {
        showDeck()
        swipe(0.54f)
        assertIdleOnFirst()
    }

    @Test
    fun guestRightSwipeSnapsBackButGuestCanPass() {
        showDeck(guest = true)
        swipe(0.95f)
        assertIdleOnFirst()
        swipe(0.05f)
        compose.runOnIdle { assertEquals(1, home.uiState.value.currentIndex) }
    }

    @Test
    fun reversalBelowDistanceThresholdCancels() {
        showDeck()
        compose.onRoot().performTouchInput {
            down(Offset(width * 0.5f, height * 0.25f))
            moveBy(Offset(70f, 0f), delayMillis = 300)
            moveBy(Offset(-50f, 0f), delayMillis = 40)
            up()
        }
        assertIdleOnFirst()
    }

    @Test
    fun swipeDuringLeavingCannotCommitAnotherCard() {
        showDeck()
        compose.mainClock.autoAdvance = false
        swipe(0.95f)
        swipe(0.05f)
        compose.mainClock.advanceTimeBy(1000)
        compose.runOnIdle {
            assertEquals(1, home.uiState.value.currentIndex)
            assertEquals(listOf(HomeItemId("first")), saves.saved)
        }
    }

    @Test
    fun fastFlingBelowDistanceThresholdCommits() {
        showDeck()
        compose.onRoot().performTouchInput {
            swipe(Offset(width * 0.5f, height * 0.25f), Offset(width * 0.6f, height * 0.25f), durationMillis = 40)
        }
        compose.runOnIdle { assertEquals(listOf(HomeItemId("first")), saves.saved) }
        compose.onNodeWithText("Listing first").assertDoesNotExist()
    }

    @Test
    fun secondPointerCancelsDrag() {
        showDeck()
        compose.onRoot().performTouchInput {
            down(0, Offset(width * 0.5f, height * 0.25f))
            moveBy(Offset(60f, 0f))
            down(1, Offset(width * 0.7f, height * 0.25f))
            up(1)
            up(0)
        }
        assertIdleOnFirst()
    }

    private fun showDeck(guest: Boolean = false) {
        val repository = DeckCharacterizationRepository(
            listOf(deckItem("first"), deckItem("second"), deckItem("third"))
        )
        val session = DeckCharacterizationSession().also { if (guest) it.unauthenticated() }
        compose.setContent {
            home = viewModel {
                HomeViewModel(
                    GetPersonalizedFeed(repository), UpdateUserPreferences(repository), session, saveListing = saves
                )
            }
            BulletinTheme { HomeScreen(home) }
        }
        compose.onNodeWithText("Listing first").assertExists()
    }

    private fun swipe(end: Float) {
        compose.onRoot().performTouchInput {
            swipe(Offset(width * 0.5f, height * 0.25f), Offset(width * end, height * 0.25f))
        }
    }

    private fun assertIdleOnFirst() {
        compose.runOnIdle {
            assertEquals(0, home.uiState.value.currentIndex)
            assertEquals(CardState.Idle, home.uiState.value.interaction.card)
            assertEquals(emptyList(), saves.saved)
        }
        compose.onNodeWithText("Listing first").assertExists()
    }
}
