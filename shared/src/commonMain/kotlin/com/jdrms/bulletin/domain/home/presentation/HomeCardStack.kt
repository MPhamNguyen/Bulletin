package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinFeedCardDefaults
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem
import com.jdrms.bulletin.domain.home.domain.model.HomeItemId
import kotlinx.coroutines.CoroutineScope

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
    LaunchedEffect(state.interaction.card, state.interaction.tray.sentReceiptId) {
        tray.settle(
            state.interaction.isTrayOpen || state.interaction.card == CardState.Leaving &&
                state.interaction.departure?.kind == HomeDepartureKind.SENT,
            trayScope
        )
    }
    val session = rememberHomeGestureSession(state.interaction, actions, motion, tray)
    HomeSentReceiptHandshake(state.interaction, actions, durationScale)
    HomeDepartureAnimation(state.interaction, current, actions, motion)
    val presentation =
        HomeStackPresentation(motion, tray, session, state.interaction, actions, durationScale, profileAnchor)
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
                presentation
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
    val profileAnchor: Rect?
)

@Composable
private fun HomeStackCard(
    item: HomeFeedItem,
    index: Int,
    front: Boolean,
    active: Boolean,
    presentation: HomeStackPresentation
) {
    val motion = presentation.motion
    val session = presentation.session
    val center = remember { mutableStateOf<Offset?>(null) }
    // Pointer coordinates belong to this stationary wrapper; only its child is transformed.
    Box(
        Modifier.fillMaxSize().onGloballyPositioned { center.value = it.boundsInWindow().center }
            .then(if (front) Modifier.homeCardGestures(session) else Modifier)
    ) {
        Box(
            Modifier.fillMaxSize().homeCardLayer(front, motion) { density ->
                homeSaveTarget(presentation.profileAnchor, center.value, density)
            }
        ) {
            HomeStackCardContents(item, index, front, active, presentation)
        }
    }
}

@Composable
private fun androidx.compose.foundation.layout.BoxScope.HomeStackCardContents(
    item: HomeFeedItem,
    index: Int,
    front: Boolean,
    active: Boolean,
    presentation: HomeStackPresentation
) {
    val motion = presentation.motion
    val tray = presentation.tray
    HomeFeedCard(
        item = item,
        cardIndex = index,
        isActive = active,
        modifier = Modifier.fillMaxSize(),
        mediaModifier = Modifier.graphicsLayer {
            if (front) translationY = -tray.progress.value * HOME_TRAY_LIFT_DP.dp.toPx()
        }
    )
    if (front && tray.visible) {
        HomeTrayPeek(
            presentation.interaction,
            presentation.actions,
            { tray.progress.value },
            presentation.durationScale,
            Modifier.align(Alignment.BottomCenter).padding(8.dp)
        )
    }
    if (front) {
        HomeSwipeFeedback(motion::saveProgress, motion::passProgress, Modifier.padding(8.dp))
    } else {
        HomeNextCardShade(motion)
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

private fun Modifier.homeCardLayer(
    front: Boolean,
    motion: HomeCardMotion,
    target: (Float) -> HomeSaveTarget?
): Modifier = graphicsLayer {
    if (front) {
        val pose = motion.pose(size.width / density, target(density))
        translationX = pose.xDp * density
        translationY = pose.yDp * density
        rotationZ = pose.rotation
        scaleX = pose.scale
        scaleY = pose.scale
        alpha = pose.alpha
        transformOrigin = TransformOrigin(0.5f, pose.pivotY)
    } else {
        val pose = homeNextCardPose(motion.promotion)
        translationX = 0f
        translationY = 0f
        rotationZ = 0f
        alpha = 1f
        scaleX = pose.scale
        scaleY = pose.scale
        transformOrigin = TransformOrigin(0.5f, 0f)
    }
}

@Composable
private fun HomeNextCardShade(motion: HomeCardMotion) {
    Box(
        Modifier.fillMaxSize().padding(8.dp).clip(MaterialTheme.shapes.large).drawBehind {
            drawRect(BulletinFeedCardDefaults.scrim.copy(alpha = homeNextCardPose(motion.promotion).shadeAlpha))
        }
    )
}
