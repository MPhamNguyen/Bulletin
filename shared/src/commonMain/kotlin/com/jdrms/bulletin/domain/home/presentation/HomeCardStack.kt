package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import kotlinx.coroutines.CoroutineScope

@Composable
internal fun HomeCardStack(state: HomeUiState, actions: HomeCardActions, durationScale: Float) {
    val current = state.feed.getOrNull(state.currentIndex) ?: return
    val motion = remember(current.id, state.deckGeneration, durationScale) { HomeCardMotion(durationScale) }
    val session = rememberHomeGestureSession(state.interaction, actions, motion)
    HomeDepartureAnimation(state.interaction, current, actions, motion)
    val cards = listOfNotNull(state.feed.getOrNull(state.currentIndex + 1), current)
    cards.forEach { item ->
        key(item.id) {
            val front = item.id == current.id
            HomeStackCard(
                item,
                state.currentIndex + if (front) 0 else 1,
                front,
                front && !state.interaction.isTrayOpen && state.interaction.card != CardState.Leaving,
                motion,
                session
            )
        }
    }
}

@Composable
private fun HomeStackCard(
    item: HomeFeedItem,
    index: Int,
    front: Boolean,
    active: Boolean,
    motion: HomeCardMotion,
    session: HomeGestureSession
) {
    // Pointer coordinates belong to this stationary wrapper; only its child is transformed.
    Box(Modifier.fillMaxSize().then(if (front) Modifier.homeCardGestures(session) else Modifier)) {
        Box(
            Modifier.fillMaxSize().graphicsLayer {
                if (front) {
                    val pose = motion.pose(size.width / density)
                    translationX = pose.xDp * density
                    translationY = pose.yDp * density
                    rotationZ = pose.rotation
                    scaleX = pose.scale
                    scaleY = pose.scale
                    alpha = pose.alpha
                    transformOrigin = TransformOrigin(0.5f, pose.pivotY)
                }
            }
        ) {
            HomeFeedCard(item = item, cardIndex = index, isActive = active, modifier = Modifier.fillMaxSize())
            if (front) HomeSwipeFeedback(motion::saveProgress, motion::passProgress, Modifier.padding(8.dp))
        }
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
    LaunchedEffect(motion, departure?.id) {
        if (departure == null || !homeDepartureMatches(departure, interaction, current.id)) return@LaunchedEffect
        motion.leave(departure)
        if (homeDepartureMatches(departure, latest, current.id)) actions.onExitCompleted(departure.id)
    }
}

@Composable
private fun rememberHomeGestureSession(
    interaction: HomeInteractionState,
    actions: HomeCardActions,
    motion: HomeCardMotion
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
            onRelease = { release -> releaseHomeCard(release, latest, actions, motion, scope) },
            onCancel = { releaseHomeCard(CardGestureResolver.Release.CANCEL, latest, actions, motion, scope) }
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
    when (homeReleaseCommand(release, interaction.saveAvailable)) {
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
