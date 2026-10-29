package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.platform.LocalInputModeManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.jdrms.bulletin.core.designsystem.LocalNavbarHeight
import com.jdrms.bulletin.core.designsystem.theme.BulletinTheme
import com.jdrms.bulletin.domain.home.application.*
import kotlinx.coroutines.CoroutineScope

/** Commands and acknowledgements are counted at the real VM boundary, not inferred from animation frames. */
internal class HomePlatformDeviceHarness {
    val ports = HomeTrayDeviceFixture()
    val commands = mutableListOf<String>()
    val exits = mutableListOf<Long>()
    val receipts = mutableListOf<Long>()
    val consumed = mutableListOf<Long>()
    val celebrations = mutableListOf<Float>()
    var onCelebration: (CoroutineScope, Float) -> Unit = { _, _ -> }
    val announcements = HomeAnnouncements()
    lateinit var actions: HomeCardActions
    var replayedConfirmation by mutableStateOf<HomeEffect.SaveConfirmed?>(null)
    var scale by mutableFloatStateOf(0f)
    var navbarSpace by mutableStateOf(0.dp)
    var viewportHeight by mutableStateOf<Dp?>(null)
    var externallyPaused by mutableStateOf(false)
    var photoPaused by mutableStateOf(false)
    var keyboard: SoftwareKeyboardController? = null
    var inputMode: InputModeManager? = null
    var tick by mutableIntStateOf(0)

    fun show(compose: ComposeContentTestRule, capabilities: Boolean = true) {
        compose.setContent { Content(capabilities) }
        compose.waitForIdle()
    }

    @Composable
    private fun Content(capabilities: Boolean) {
        val scope = rememberCoroutineScope()
        ports.home = viewModel {
            val repo = DeckCharacterizationRepository(
                listOf(deckItem("first"), deckItem("second"), deckItem("third"))
            )
            HomeViewModel(
                GetPersonalizedFeed(repo), UpdateUserPreferences(repo), DeckCharacterizationSession(),
                sendQuickMessage = if (capabilities) {
                    SendQuickMessage(ports)
                } else {
                    null
                },
                saveListing = if (capabilities) ports else null
            )
        }
        actions = remember { recordingActions(ports.home) }
        val state by ports.home.uiState.collectAsState()

        @Suppress("UNUSED_VARIABLE")
        val recompose = tick
        BulletinTheme {
            CompositionLocalProvider(
                LocalHomeAnnouncements provides announcements,
                LocalNavbarHeight provides navbarSpace
            ) {
                Deck(state)
                HomeConfirmedSaveCelebration(
                    state.interaction.copy(
                        pendingEffects = state.interaction.pendingEffects + listOfNotNull(replayedConfirmation)
                    ),
                    actions,
                    Rect(0f, 0f, 48f, 48f),
                    scale
                ) {
                    celebrations += it
                    onCelebration(scope, it)
                }
            }
        }
    }

    @Composable
    private fun Deck(state: HomeUiState) {
        keyboard = LocalSoftwareKeyboardController.current
        inputMode = LocalInputModeManager.current
        HomeStateAnnouncements(announcements, state.interaction)
        Box(Modifier.fillMaxSize().padding(bottom = navbarSpace)) {
            HomeKeyboardViewport(
                Modifier.then(homeTrayDismissal(state.interaction.card, actions)),
                externallyPaused
            ) { paused ->
                SideEffect { photoPaused = paused }
                val viewport = viewportHeight?.let { Modifier.fillMaxWidth().height(it) } ?: Modifier.fillMaxSize()
                Box(viewport.testTag("home_test_viewport")) {
                    HomeCardStack(state, actions, scale, photoPlaybackPaused = paused)
                }
            }
        }
    }

    private fun recordingActions(home: HomeViewModel): HomeCardActions = object : HomeCardActions by home {
        override fun sendMessage() {
            announcements.beginSend()
            home.sendMessage()
        }
        override fun saveCurrentCard() {
            commands += "save"
            home.saveCurrentCard()
        }
        override fun passCurrentCard() {
            commands += "pass"
            home.passCurrentCard()
        }
        override fun openTray() {
            commands += "open"
            home.openTray()
        }
        override fun closeTray() {
            commands += "close"
            home.closeTray()
        }
        override fun onExitCompleted(departureId: Long) {
            exits += departureId
            home.onExitCompleted(departureId)
        }
        override fun onSentReceiptCompleted(receiptId: Long) {
            receipts += receiptId
            home.onSentReceiptCompleted(receiptId)
        }
        override fun onEffectConsumed(id: Long) {
            consumed += id
            home.onEffectConsumed(id)
        }
    }
}
