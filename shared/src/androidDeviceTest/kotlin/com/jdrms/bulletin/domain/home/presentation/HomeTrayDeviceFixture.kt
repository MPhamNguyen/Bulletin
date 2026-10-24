package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.StateRestorationTester
import com.jdrms.bulletin.domain.home.infrastructure.repository.InMemorySwipeDecisionRepository
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.designsystem.BulletinTheme
import com.jdrms.bulletin.domain.home.application.SaveListingPort
import com.jdrms.bulletin.domain.home.application.ContactSellerPort
import com.jdrms.bulletin.domain.home.application.GetPersonalizedFeed
import com.jdrms.bulletin.domain.home.application.SendQuickMessage
import com.jdrms.bulletin.domain.home.application.UpdateUserPreferences
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.SellerMessage

internal class HomeTrayDeviceFixture : ContactSellerPort, SaveListingPort {
    lateinit var home: HomeViewModel
    lateinit var content: @Composable () -> Unit
    val decisions = InMemorySwipeDecisionRepository()
    val saves = mutableListOf<HomeItemId>()
    val sends = mutableListOf<Pair<HomeItemId, String>>()
    var sendResult: suspend () -> Result<Unit> = { Result.Success(Unit) }
    var saveResult: suspend () -> Result<Unit> = { Result.Success(Unit) }

    /** Zero-motion variant: intentionally bypasses interpolation, retaining gesture/state coverage. */
    fun showWithZeroMotion(
        compose: ComposeContentTestRule,
        description: String = "Available",
        anchor: Rect? = null,
        onCelebrate: (Float) -> Unit = {},
        restoration: StateRestorationTester? = null,
        modifier: Modifier = Modifier
    ) = show(compose, description, 0f, anchor, onCelebrate, restoration, modifier)

    /** Prefer showWithZeroMotion for tests that intentionally skip interpolation. */
    fun show(
        compose: ComposeContentTestRule,
        description: String = "Available",
        durationScale: Float = 0f,
        anchor: Rect? = null,
        onCelebrate: (Float) -> Unit = {},
        restoration: StateRestorationTester? = null,
        modifier: Modifier = Modifier
    ) {
        val repository = DeckCharacterizationRepository(listOf(
            deckItem("first").copy(description = description), deckItem("second"), deckItem("third")
        ))
        val session = DeckCharacterizationSession()
        content = {
            home = viewModel {
                HomeViewModel(GetPersonalizedFeed(repository), UpdateUserPreferences(repository), session,
                    sendQuickMessage = SendQuickMessage(this@HomeTrayDeviceFixture), saveListing = this@HomeTrayDeviceFixture,
                    swipeDecisionRepository = decisions)
            }
            BulletinTheme {
                HomeScreen(home, modifier = modifier, motionDurationScale = durationScale,
                    profileAnchorInWindow = anchor, onSaveCelebration = onCelebrate)
            }
        }
        if (restoration == null) compose.setContent(content) else restoration.setContent(content)
        compose.onNodeWithText("Listing first").assertExists()
    }

    override suspend fun save(userId: String, listingId: HomeItemId): Result<Unit> {
        saves += listingId
        return saveResult()
    }

    override suspend fun send(userId: String, listingId: HomeItemId, message: SellerMessage): Result<Unit> {
        sends += listingId to message.text
        return sendResult()
    }
}
