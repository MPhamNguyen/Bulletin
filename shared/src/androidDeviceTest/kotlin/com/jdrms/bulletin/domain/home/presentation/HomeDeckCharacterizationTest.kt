package com.jdrms.bulletin.domain.home.presentation

import androidx.activity.ComponentActivity
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.hasProgressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipe
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jdrms.bulletin.core.designsystem.BulletinTheme
import com.jdrms.bulletin.domain.home.application.GetPersonalizedFeed
import com.jdrms.bulletin.domain.home.application.UpdateUserPreferences
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedCursor
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedPage
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import kotlinx.coroutines.CompletableDeferred
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
class HomeDeckCharacterizationTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun rightSwipeAdvancesExactlyOneCard() {
        showDeck()

        swipeCard(right = true)

        assertAdvancedOnce()
    }

    @Test
    fun leftSwipeAdvancesExactlyOneCard() {
        showDeck()

        swipeCard(right = false)

        assertAdvancedOnce()
    }

    @Test
    fun cardAdvancesOnlyAfterExitAnimationCompletes() {
        showDeck()
        compose.mainClock.autoAdvance = false

        swipeCard(right = true)
        compose.mainClock.advanceTimeBy(64)
        compose.onNodeWithText("Listing first").assertExists()

        compose.mainClock.advanceTimeBy(800)
        assertAdvancedOnce()
    }

    @Test
    fun dragBelowThresholdSnapsBackWithoutAdvancing() {
        showDeck()

        compose.onRoot().performTouchInput {
            swipe(Offset(width * 0.5f, height * 0.25f), Offset(width * 0.55f, height * 0.25f))
        }

        compose.onNodeWithText("Listing first").assertExists()
        compose.onNodeWithText("Listing third").assertDoesNotExist()
    }

    @Test
    fun cancelledDragKeepsCurrentCard() {
        showDeck()

        compose.onRoot().performTouchInput {
            down(Offset(width * 0.5f, height * 0.25f))
            moveBy(Offset(width * 0.35f, 0f))
            cancel()
        }

        compose.onNodeWithText("Listing first").assertExists()
        compose.onNodeWithText("Listing third").assertDoesNotExist()
    }

    @Test
    fun exhaustedDeckStopsAndStartOverReplaysWithoutFetching() {
        val repository = DeckCharacterizationRepository(listOf(deckItem("first")))
        showDeck(repository)

        swipeCard(right = false)
        compose.onNodeWithText("You're All Caught Up").assertExists()
        compose.onNodeWithText("Listing first").assertDoesNotExist()

        compose.onNodeWithText("Start Over").performClick()
        compose.onNodeWithText("Listing first").assertExists()
        compose.onNodeWithText("You're All Caught Up").assertDoesNotExist()
        compose.runOnIdle { assertEquals(listOf<HomeFeedCursor?>(null), repository.requestedCursors) }
    }

    @Test
    fun exhaustedDeckShowsLoadingWhileAppendingThenContinues() {
        val cursor = HomeFeedCursor(Instant.fromEpochMilliseconds(100), HomeItemId("first"))
        val append = CompletableDeferred<HomeFeedPage>()
        val repository = DeckCharacterizationRepository(listOf(deckItem("first")), cursor, append)
        showDeck(repository)
        compose.runOnIdle { assertEquals(listOf(null, cursor), repository.requestedCursors) }

        swipeCard(right = true)

        compose.onNodeWithText("You're All Caught Up").assertDoesNotExist()
        compose.onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate)).assertExists()
        compose.runOnIdle { append.complete(HomeFeedPage(listOf(deckItem("second")), null)) }
        compose.onNodeWithText("Listing second").assertExists()
        compose.onNodeWithText("You're All Caught Up").assertDoesNotExist()
        swipeCard(right = false)
        compose.onNodeWithText("You're All Caught Up").assertExists()
    }

    private fun showDeck(
        repository: DeckCharacterizationRepository = DeckCharacterizationRepository(
            listOf(deckItem("first"), deckItem("second"), deckItem("third"))
        )
    ) {
        compose.setContent {
            val home: HomeViewModel = viewModel {
                HomeViewModel(
                    GetPersonalizedFeed(repository),
                    UpdateUserPreferences(repository),
                    DeckCharacterizationSession(),
                    saveListing = DeckCharacterizationSavePort()
                )
            }
            BulletinTheme { HomeScreen(home) }
        }
        compose.onNodeWithText("Listing first").assertExists()
    }

    private fun swipeCard(right: Boolean) {
        compose.onRoot().performTouchInput {
            val endX = if (right) width * 0.95f else width * 0.05f
            swipe(Offset(width * 0.5f, height * 0.25f), Offset(endX, height * 0.25f))
        }
    }

    private fun assertAdvancedOnce() {
        compose.onNodeWithText("Listing first").assertDoesNotExist()
        compose.onNodeWithText("Listing second").assertExists()
        compose.onNodeWithText("Listing third").assertExists()
        compose.onNodeWithText("You're All Caught Up").assertDoesNotExist()
    }
}
