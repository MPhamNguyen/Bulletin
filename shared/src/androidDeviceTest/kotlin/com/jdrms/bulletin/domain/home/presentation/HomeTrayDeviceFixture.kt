package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.designsystem.BulletinTheme
import com.jdrms.bulletin.domain.home.application.ContactSellerPort
import com.jdrms.bulletin.domain.home.application.GetPersonalizedFeed
import com.jdrms.bulletin.domain.home.application.SendQuickMessage
import com.jdrms.bulletin.domain.home.application.UpdateUserPreferences
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import com.jdrms.bulletin.domain.home.domain.model.SellerMessage

internal class HomeTrayDeviceFixture : ContactSellerPort {
    lateinit var home: HomeViewModel
    val sends = mutableListOf<Pair<HomeItemId, String>>()
    var sendResult: suspend () -> Result<Unit> = { Result.Success(Unit) }

    fun show(
        compose: ComposeContentTestRule,
        description: String = "Available",
        durationScale: Float = 0f
    ) {
        val repository = DeckCharacterizationRepository(listOf(
            deckItem("first").copy(description = description), deckItem("second"), deckItem("third")
        ))
        val session = DeckCharacterizationSession()
        compose.setContent {
            home = viewModel {
                HomeViewModel(GetPersonalizedFeed(repository), UpdateUserPreferences(repository), session,
                    sendQuickMessage = SendQuickMessage(this@HomeTrayDeviceFixture), saveListing = DeckCharacterizationSavePort())
            }
            BulletinTheme { HomeScreen(home, motionDurationScale = durationScale) }
        }
        compose.onNodeWithText("Listing first").assertExists()
    }

    override suspend fun send(userId: String, listingId: HomeItemId, message: SellerMessage): Result<Unit> {
        sends += listingId to message.text
        return sendResult()
    }
}
