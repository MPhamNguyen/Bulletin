package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import bulletin.shared.generated.resources.*
import com.jdrms.bulletin.domain.home.domain.model.CardAction
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import kotlinx.coroutines.CoroutineScope
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun HomeCardStack(
    state: HomeUiState,
    actions: HomeCardActions,
    durationScale: Float,
    profileAnchor: Rect? = null,
    photoPlaybackPaused: Boolean = false
) {
    val current = state.feed.getOrNull(state.currentIndex) ?: return
    val motion = remember(current.id, state.deckGeneration, durationScale) { HomeCardMotion(durationScale) }
    val tray = remember(motion) { HomeTrayMotion(durationScale) }
    val trayScope = rememberCoroutineScope()
    LaunchedEffect(tray, state.interaction.card, state.interaction.tray.sentReceiptId) {
        tray.settle(
            state.interaction.isTrayOpen || state.interaction.card == CardState.Leaving &&
                state.interaction.departure?.kind == HomeDepartureKind.SENT,
            trayScope
        )
    }
    val cardFocus = remember { FocusRequester() }
    val composerFocus = remember { FocusRequester() }
    val actionFocus = remember(state.deckGeneration) { HomeActionFocus() }
    HomeTrayFocus(state.interaction.card, cardFocus, composerFocus, actionFocus.restoreAction == null)
    HomeTrayKeyboard(state.interaction.isTrayOpen)
    val session = rememberHomeGestureSession(state.interaction, actions, motion, tray)
    HomeSentReceiptHandshake(state.interaction, actions, durationScale)
    HomeDepartureAnimation(state.interaction, current, actions, motion)
    val presentation =
        HomeStackPresentation(motion, tray, session, state.interaction, actions, durationScale, profileAnchor)
    HomeStackDeck(state, current, photoPlaybackPaused, presentation, cardFocus, composerFocus, actionFocus)
}

@Composable
private fun HomeStackDeck(
    state: HomeUiState,
    current: HomeFeedItem,
    photoPlaybackPaused: Boolean,
    presentation: HomeStackPresentation,
    cardFocus: FocusRequester,
    composerFocus: FocusRequester,
    actionFocus: HomeActionFocus
) {
    var actionHeight by remember { mutableStateOf(64.dp) }
    val density = LocalDensity.current
    Box(Modifier.fillMaxSize()) {
        HomeDeckCards(
            state,
            current,
            photoPlaybackPaused,
            presentation.copy(actionHeight = actionHeight),
            cardFocus,
            composerFocus
        )
        HomeCardActionRow(
            state.interaction.availableActions.toButtonUi(),
            { action ->
                actionFocus.prepare(action)
                presentation.actions.perform(action)
            },
            presentation.tray.progress.value,
            state.interaction.isTrayOpen || state.interaction.card == CardState.Peeking,
            Modifier.align(Alignment.BottomCenter).padding(horizontal = 20.dp).padding(bottom = 20.dp)
                .onSizeChanged { actionHeight = with(density) { it.height.toDp() } + 12.dp },
            reservedHeight = actionHeight - 12.dp,
            actionModifier = { action, enabled -> actionFocus.modifier(action, enabled) }
        )
    }
}

@Composable
private fun HomeDeckCards(
    state: HomeUiState,
    current: HomeFeedItem,
    photoPlaybackPaused: Boolean,
    presentation: HomeStackPresentation,
    cardFocus: FocusRequester,
    composerFocus: FocusRequester
) {
    val cards = listOfNotNull(state.feed.getOrNull(state.currentIndex + 1), current)
    cards.forEach { item ->
        key(item.id) {
            val front = item.id == current.id
            HomeStackCard(
                item,
                state.currentIndex + if (front) 0 else 1,
                front,
                front && !photoPlaybackPaused && !state.interaction.isTrayOpen &&
                    state.interaction.card != CardState.Leaving,
                presentation,
                cardFocus,
                composerFocus
            )
        }
    }
}

private data class HomeStackPresentation(
    val motion: HomeCardMotion,
    val tray: HomeTrayMotion,
    val session: HomeGestureSession,
    val interaction: HomeInteractionState,
    val actions: HomeCardActions,
    val durationScale: Float,
    val profileAnchor: Rect?,
    val actionHeight: Dp = 64.dp
)

@Composable
private fun HomeStackCard(
    item: HomeFeedItem,
    index: Int,
    front: Boolean,
    active: Boolean,
    presentation: HomeStackPresentation,
    cardFocus: FocusRequester,
    composerFocus: FocusRequester
) {
    val motion = presentation.motion
    val session = presentation.session
    val center = remember { mutableStateOf<Offset?>(null) }
    // Pointer coordinates belong to this stationary wrapper; only its child is transformed.
    Box(
        Modifier.fillMaxSize().onGloballyPositioned { center.value = it.boundsInWindow().center }
            .then(
                if (front) {
                    Modifier.homeCardGestures(session).then(
                        homeCardAccessibility(item, presentation.interaction, presentation.actions, cardFocus)
                    )
                } else {
                    Modifier.semantics { hideFromAccessibility() }
                }
            )
    ) {
        Box(
            Modifier.fillMaxSize().homeCardLayer(front, motion) { density ->
                homeSaveTarget(presentation.profileAnchor, center.value, density)
            }
        ) {
            HomeStackCardContents(item, index, front, active, presentation, composerFocus)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.HomeStackCardContents(
    item: HomeFeedItem,
    index: Int,
    front: Boolean,
    active: Boolean,
    presentation: HomeStackPresentation,
    composerFocus: FocusRequester
) {
    HomeFeedCard(
        item = item,
        cardIndex = index,
        isActive = active,
        modifier = Modifier.fillMaxSize(),
        informationBottomPadding = presentation.actionHeight,
        mediaModifier = if (front) {
            Modifier.homeTrayLift(homeTraySurface) { presentation.tray.progress.value }
        } else {
            Modifier
        },
        trayProgress = { if (front) presentation.tray.progress.value else 0f }
    )
    if (front && presentation.tray.visible) {
        HomeTrayPeek(
            presentation.interaction,
            presentation.actions,
            { presentation.tray.progress.value },
            presentation.durationScale,
            Modifier.align(Alignment.BottomCenter).padding(8.dp),
            composerFocus
        )
    }
    if (front) {
        HomeSwipeFeedback(presentation.motion::saveProgress, presentation.motion::passProgress, Modifier.padding(8.dp))
    } else {
        HomeNextCardShade(presentation.motion)
    }
}

@Composable
private fun HomeDepartureAnimation(
    interaction: HomeInteractionState,
    current: HomeFeedItem,
    actions: HomeCardActions,
    motion: HomeCardMotion
) {
    val latest by rememberUpdatedState(interaction)
    val departure = interaction.departure
    val announcements = LocalHomeAnnouncements.current
    val passed = stringResource(Res.string.home_passed)
    LaunchedEffect(motion, departure?.id) {
        if (departure == null || !homeDepartureMatches(departure, interaction, current.id)) return@LaunchedEffect
        if (departure.kind == HomeDepartureKind.PASS) announcements.once("pass:${departure.id}", passed)
        motion.leave(departure)
        if (homeDepartureMatches(departure, latest, current.id)) actions.onExitCompleted(departure.id)
    }
}

@Composable
private fun rememberHomeGestureSession(
    interaction: HomeInteractionState,
    actions: HomeCardActions,
    motion: HomeCardMotion,
    tray: HomeTrayMotion
): HomeGestureSession {
    val latest by rememberUpdatedState(interaction)
    val scope = rememberCoroutineScope()
    return remember(actions, motion, scope) {
        HomeGestureSession(
            interaction = { latest },
            onBegin = motion::begin,
            onAxis = { axis ->
                actions.onGestureEvent(if (axis == CardGestureResolver.Axis.X) CardEvent.DRAG_X else CardEvent.DRAG_Y)
            },
            onDrag = motion::drag,
            onRelease = { release ->
                releaseHomeCard(release, latest, actions, motion, scope)
                tray.settle(latest.isTrayOpen, scope)
            },
            onCancel = {
                releaseHomeCard(CardGestureResolver.Release.CANCEL, latest, actions, motion, scope)
                tray.settle(latest.isTrayOpen, scope)
            },
            onVerticalDrag = { dy, open -> if (latest.messageAvailable) tray.follow(dy, open, scope) },
            isTapOnMedia = { yDp, heightDp -> yDp < heightDp - HOME_TRAY_LIFT_DP * tray.progress.value }
        )
    }
}

private fun releaseHomeCard(
    release: CardGestureResolver.Release,
    interaction: HomeInteractionState,
    actions: HomeCardActions,
    motion: HomeCardMotion,
    scope: CoroutineScope
) {
    when (homeReleaseCommand(release, interaction.availableActions.allows(CardAction.Save))) {
        HomeReleaseCommand.SAVE -> actions.saveCurrentCard()
        HomeReleaseCommand.PASS -> actions.passCurrentCard()
        HomeReleaseCommand.CANCEL -> {
            motion.snapBack(scope)
            actions.onGestureEvent(CardEvent.CANCEL)
        }
        HomeReleaseCommand.OPEN -> actions.openTray()
        HomeReleaseCommand.CLOSE -> actions.closeTray()
    }
}

internal fun homeDepartureMatches(
    departure: HomeDeparture,
    interaction: HomeInteractionState,
    currentId: HomeItemId
): Boolean = interaction.card == CardState.Leaving && interaction.departure == departure &&
    departure.listingId == currentId
