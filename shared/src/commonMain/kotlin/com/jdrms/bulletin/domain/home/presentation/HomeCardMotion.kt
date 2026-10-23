package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

/** Transient motion state, scoped to one mounted listing, never to the feed ViewModel. */
internal class HomeCardMotion(val reducedMotion: Boolean) {
    var offsetX by mutableFloatStateOf(0f)
    var trayProgress by mutableFloatStateOf(0f)
    var nextProgress by mutableFloatStateOf(0f)
    var exitProgress by mutableFloatStateOf(0f)
    var saveTargetX by mutableFloatStateOf(0f)
    var saveTargetY by mutableFloatStateOf(0f)
    var locked by mutableStateOf(false)
    var messageBusy by mutableStateOf(false)
    var saveCelebrating by mutableStateOf(false)
    val inputLocked get() = locked || messageBusy

    suspend fun releaseHorizontal(commit: Int, width: Float, onComplete: (Int) -> Unit) =
        releaseHorizontal(commit, width, {}, onComplete)

    suspend fun releaseHorizontal(
        commit: Int,
        width: Float,
        onCommit: (Int) -> Unit,
        onComplete: (Int) -> Unit
    ) {
        if (commit == 0) {
            if (reducedMotion) {
                offsetX = 0f
            } else {
                animate(
                    offsetX,
                    0f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessMedium
                    )
                ) { value, _ -> offsetX = value }
            }
            nextProgress = 0f
            locked = false
        } else {
            saveCelebrating = commit > 0
            onCommit(commit)
            try {
                animateHorizontalExit(commit, width)
            } finally {
                // Refresh/navigation can unmount a committed card before its animation finishes.
                if (commit < 0) onComplete(commit)
            }
            if (commit > 0) onComplete(commit)
        }
    }

    private suspend fun animateHorizontalExit(commit: Int, width: Float) {
        if (reducedMotion) {
            if (commit > 0) {
                animate(exitProgress, 1f, animationSpec = tween(180)) { value, _ -> exitProgress = value }
            }
            return
        }
        coroutineScope {
            launch {
                val destination = if (commit > 0 && saveTargetX != 0f) saveTargetX else width * 1.35f * commit
                animate(
                    offsetX,
                    destination,
                    animationSpec = tween(600, easing = FastOutLinearInEasing)
                ) { v, _ -> offsetX = v }
            }
            if (commit > 0) {
                launch {
                    animate(
                        0f,
                        1f,
                        animationSpec = tween(600, easing = FastOutLinearInEasing)
                    ) { v, _ -> exitProgress = v }
                }
                launch { animate(nextProgress, 1f, animationSpec = tween(600)) { v, _ -> nextProgress = v } }
            } else {
                launch { animate(nextProgress, 1f, animationSpec = tween(250)) { v, _ -> nextProgress = v } }
            }
        }
    }

    suspend fun settleTray(target: Float) {
        if (reducedMotion) {
            trayProgress = target
        } else {
            animate(trayProgress, target, animationSpec = tween(350)) { value, _ -> trayProgress = value }
        }
        locked = false
    }

    suspend fun exitAfterMessage(onComplete: () -> Unit) {
        if (locked) return
        locked = true
        if (!reducedMotion) {
            coroutineScope {
                launch { animate(0f, 1f, animationSpec = tween(600)) { v, _ -> exitProgress = v } }
                launch { animate(nextProgress, 1f, animationSpec = tween(600)) { v, _ -> nextProgress = v } }
            }
        }
        onComplete()
    }
}

/** Direction must agree with travel so a reversal cannot commit the opposite action. */
internal fun homeSwipeCommit(distance: Float, velocity: Float, threshold: Float, flingThreshold: Float): Int = when {
    distance <= -threshold || (distance < 0f && velocity <= -flingThreshold) -> -1
    distance >= threshold || (distance > 0f && velocity >= flingThreshold) -> 1
    else -> 0
}
