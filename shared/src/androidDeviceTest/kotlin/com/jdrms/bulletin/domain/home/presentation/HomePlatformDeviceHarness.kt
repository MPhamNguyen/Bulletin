package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jdrms.bulletin.core.designsystem.BulletinTheme
import com.jdrms.bulletin.domain.home.application.*
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId

/** Commands and acknowledgements are counted at the real VM boundary, not inferred from animation frames. */
internal class HomePlatformDeviceHarness {
    val ports = HomeTrayDeviceFixture()
    val commands = mutableListOf<String>()
    val exits = mutableListOf<Long>()
    val receipts = mutableListOf<Long>()
    val consumed = mutableListOf<Long>()
    val celebrations = mutableListOf<Float>()
    val announcements = HomeAnnouncements()
    lateinit var actions: HomeCardActions
    var replayedConfirmation by mutableStateOf<HomeEffect.SaveConfirmed?>(null)
    var scale by mutableFloatStateOf(0f)
    var tick by mutableIntStateOf(0)

    fun show(compose: ComposeContentTestRule, capabilities: Boolean = true) {
        compose.setContent {
            ports.home = viewModel {
                val repo = DeckCharacterizationRepository(listOf(deckItem("first"), deckItem("second"), deckItem("third")))
                HomeViewModel(GetPersonalizedFeed(repo), UpdateUserPreferences(repo), DeckCharacterizationSession(),
                    sendQuickMessage = if (capabilities) SendQuickMessage(ports) else null,
                    saveListing = if (capabilities) ports else null)
            }
            actions = remember { recordingActions(ports.home) }
            val state by ports.home.uiState.collectAsState()
            @Suppress("UNUSED_VARIABLE") val recompose = tick
            BulletinTheme {
                CompositionLocalProvider(LocalHomeAnnouncements provides announcements) {
                    HomeStateAnnouncements(announcements, state.interaction)
                    Box(Modifier.fillMaxSize().then(homeTrayDismissal(state.interaction.card, actions))) { HomeCardStack(state, actions, scale) }
                    HomeConfirmedSaveCelebration(state.interaction.copy(pendingEffects = state.interaction.pendingEffects + listOfNotNull(replayedConfirmation)), actions, Rect(0f, 0f, 48f, 48f), scale) {
                        celebrations += it
                    }
                }
            }
        }
        compose.waitForIdle()
    }

    private fun recordingActions(home: HomeViewModel): HomeCardActions = object : HomeCardActions by home {
        override fun sendMessage() { announcements.beginSend(); home.sendMessage() }
        override fun saveCurrentCard() { commands += "save"; home.saveCurrentCard() }
        override fun passCurrentCard() { commands += "pass"; home.passCurrentCard() }
        override fun openTray() { commands += "open"; home.openTray() }
        override fun closeTray() { commands += "close"; home.closeTray() }
        override fun onExitCompleted(departureId: Long) { exits += departureId; home.onExitCompleted(departureId) }
        override fun onSentReceiptCompleted(receiptId: Long) { receipts += receiptId; home.onSentReceiptCompleted(receiptId) }
        override fun onEffectConsumed(id: Long) { consumed += id; home.onEffectConsumed(id) }
    }
}
