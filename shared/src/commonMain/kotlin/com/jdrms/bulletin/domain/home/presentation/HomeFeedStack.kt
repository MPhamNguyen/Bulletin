package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem
import com.jdrms.bulletin.domain.home.presentation.quickmessage.QUICK_MESSAGE_TRAY_HEIGHT
import com.jdrms.bulletin.domain.home.presentation.quickmessage.QuickMessageActions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal data class HomeFeedActions(
    val onSendQuickMessage: suspend (String) -> Boolean,
    val onSave: () -> Unit,
    val onPass: () -> Unit,
    val onAdvance: () -> Unit
) {
    fun commitSwipe(direction: Int) { if (direction > 0) onSave() }
    fun completeSwipe(direction: Int) { if (direction < 0) onPass() else onAdvance() }
}

/** Shared card stack accepts reduced motion without depending on any Android API. */
@Composable
internal fun HomeFeedStack(
    item: HomeFeedItem,
    nextItem: HomeFeedItem?,
    reducedMotion: Boolean,
    profileTargetPosition: Offset,
    actions: HomeFeedActions
) {
    val density = LocalDensity.current
    val motion = remember(reducedMotion) { HomeCardMotion(reducedMotion) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat()
        val geometry = with(density) {
            HomeGestureGeometry(
                width,
                90.dp.toPx(),
                90.dp.toPx(),
                600.dp.toPx(),
                QUICK_MESSAGE_TRAY_HEIGHT.toPx(),
                24.dp.toPx()
            )
        }
        if (nextItem != null) HomeNextFeedCard(nextItem, motion)
        HomeFrontFeedCard(item, motion, geometry, height, profileTargetPosition, actions)
    }
}

@Composable
private fun HomeNextFeedCard(nextItem: HomeFeedItem, motion: HomeCardMotion) {
    val density = LocalDensity.current
    HomeFeedCard(
        item = nextItem,
        state = HomeFeedCardState(isActive = false),
        modifier = Modifier.fillMaxSize().graphicsLayer {
            val progress = motion.nextProgress
            translationY = with(density) { (-12.dp + 12.dp * progress).toPx() }
            scaleX = 0.94f + 0.06f * progress
            scaleY = scaleX
            alpha = 0.7f + 0.3f * progress
        }
    )
}

@Composable
private fun HomeFrontFeedCard(
    item: HomeFeedItem,
    motion: HomeCardMotion,
    geometry: HomeGestureGeometry,
    height: Float,
    profileTargetPosition: Offset,
    actions: HomeFeedActions
) {
    val scope = rememberCoroutineScope()
    val haptics = LocalHapticFeedback.current
    var quickSendTrigger by remember { mutableIntStateOf(0) }
    LaunchedEffect(motion.saveCelebrating) {
        if (motion.saveCelebrating) {
            kotlinx.coroutines.delay(650)
            motion.saveCelebrating = false
        }
    }

    HomeFeedCard(
        item = item,
        state = HomeFeedCardState(
            expansionProgress = motion.trayProgress,
            quickSendTrigger = quickSendTrigger,
            passProgress = (-motion.offsetX / geometry.passThreshold).coerceIn(0f, 1f),
            saveProgress = (motion.offsetX / geometry.saveThreshold).coerceIn(0f, 1f)
        ),
        actions = QuickMessageActions(
            onSend = actions.onSendQuickMessage,
            onSending = { motion.messageBusy = true },
            onSendFailed = { motion.messageBusy = false },
            onAnimationComplete = { scope.launch { motion.exitAfterMessage(actions.onAdvance) } }
        ),
        modifier = Modifier.fillMaxSize().zIndex(
            if (motion.saveCelebrating) 101f else 0f
        ).onGloballyPositioned { coordinates ->
            val origin = coordinates.positionInRoot()
            motion.saveTargetX = profileTargetPosition.x - origin.x - coordinates.size.width / 2f
            motion.saveTargetY = profileTargetPosition.y - origin.y - coordinates.size.height / 2f
        }.homeCardTransform(motion, geometry.width, height)
            .homeCardGestures(
                motion,
                geometry,
                scope,
                onSwipeCommit = { direction ->
                    if (direction > 0) {
                        scope.launch {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            delay(45)
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        }
                        actions.commitSwipe(direction)
                    }
                },
                onSwipeComplete = actions::completeSwipe,
                onQuickSend = { quickSendTrigger++ }
            )
    )
    if (motion.saveCelebrating && !motion.reducedMotion) {
        HomeSaveCelebration(Modifier.fillMaxSize(), motion.reducedMotion)
    }
}

private fun Modifier.homeCardTransform(motion: HomeCardMotion, width: Float, height: Float): Modifier = graphicsLayer {
    translationX = motion.offsetX
    translationY = when {
        motion.saveCelebrating && motion.reducedMotion -> 0f
        motion.saveCelebrating -> motion.saveTargetY * motion.exitProgress
        else -> -motion.exitProgress * height * 1.18f
    }
    val saveScale = if (motion.saveCelebrating && !motion.reducedMotion) {
        1f - motion.exitProgress * 0.95f
    } else {
        1f
    }
    scaleX = saveScale
    scaleY = saveScale
    rotationZ = if (motion.reducedMotion) {
        0f
    } else {
        (motion.offsetX / width * 15f).coerceIn(-20f, 20f) + motion.exitProgress * 10f
    }
    alpha = 1f - motion.exitProgress
    transformOrigin = TransformOrigin.Center
}
