package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.foundation.gestures.drag
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import kotlin.math.abs

internal data class HomeGestureGeometry(
    val width: Float,
    val passThreshold: Float,
    val saveThreshold: Float,
    val flingThreshold: Float,
    val trayHeight: Float,
    val edgeGuard: Float
)

internal fun Modifier.homeCardGestures(
    motion: HomeCardMotion,
    geometry: HomeGestureGeometry,
    scope: CoroutineScope,
    onSwipeCommit: (Int) -> Unit,
    onSwipeComplete: (Int) -> Unit,
    onQuickSend: () -> Unit
): Modifier = pointerInput(motion, geometry) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (motion.inputLocked || down.position.x < geometry.edgeGuard ||
            down.position.x > size.width - geometry.edgeGuard
        ) {
            return@awaitEachGesture
        }
        val gesture = HomeCardDrag(motion, geometry, down)
        val start = awaitTouchSlopOrCancellation(down.id) { change, overSlop ->
            gesture.start(change, overSlop)
            change.consume()
        } ?: return@awaitEachGesture
        val completed = drag(start.id) { change ->
            gesture.move(change)
            change.consume()
        }
        currentEvent.changes.firstOrNull { it.id == start.id }?.let { gesture.recordRelease(it) }
        gesture.finish(completed, scope, onSwipeCommit, onSwipeComplete, onQuickSend)
    }
}

/** One axis is selected once, after slop; cancellation always restores the card. */
private class HomeCardDrag(
    private val motion: HomeCardMotion,
    private val geometry: HomeGestureGeometry,
    down: PointerInputChange
) {
    private val tracker = VelocityTracker().apply { addPosition(down.uptimeMillis, down.position) }
    private val startProgress = motion.trayProgress
    private var horizontal = false
    private var distance = Offset.Zero
    private val ignoreHorizontal get() = horizontal && startProgress > 0f

    fun start(change: PointerInputChange, overSlop: Offset) {
        horizontal = abs(overSlop.x) > abs(overSlop.y)
        distance = overSlop
        tracker.addPosition(change.uptimeMillis, change.position)
        updateMotion(overSlop)
    }

    fun recordRelease(change: PointerInputChange) {
        tracker.addPosition(change.uptimeMillis, change.position)
    }

    fun move(change: PointerInputChange) {
        val delta = change.position - change.previousPosition
        distance += delta
        tracker.addPosition(change.uptimeMillis, change.position)
        updateMotion(delta)
    }

    private fun updateMotion(delta: Offset) {
        if (horizontal && !ignoreHorizontal) {
            motion.offsetX += delta.x
            motion.nextProgress = (abs(motion.offsetX) / geometry.passThreshold).coerceIn(0f, 1f) * 0.55f
        } else if (!horizontal) {
            motion.trayProgress = (startProgress - distance.y / geometry.trayHeight).coerceIn(0f, 1f)
        }
    }

    fun finish(
        completed: Boolean,
        scope: CoroutineScope,
        onSwipeCommit: (Int) -> Unit,
        onSwipeComplete: (Int) -> Unit,
        onQuickSend: () -> Unit
    ) {
        if (ignoreHorizontal) return
        motion.locked = true
        val velocity = tracker.calculateVelocity()
        if (horizontal) {
            val commit = if (completed) {
                homeSwipeCommit(
                    motion.offsetX,
                    velocity.x,
                    if (motion.offsetX > 0f) geometry.saveThreshold else geometry.passThreshold,
                    geometry.flingThreshold
                )
            } else {
                0
            }
            scope.launch(start = CoroutineStart.UNDISPATCHED) {
                motion.releaseHorizontal(commit, geometry.width, onSwipeCommit, onSwipeComplete)
            }
        } else {
            val target = trayReleaseTarget(startProgress, distance.y, velocity.y, completed, onQuickSend)
            scope.launch { motion.settleTray(target) }
        }
    }
}

private fun trayReleaseTarget(
    startProgress: Float,
    distance: Float,
    velocity: Float,
    completed: Boolean,
    onQuickSend: () -> Unit
): Float {
    if (!completed) return startProgress
    if (startProgress <= 0.5f && -distance > 60f && -velocity > 1300f) {
        onQuickSend()
        return 1f
    }
    return if (startProgress > 0.5f) {
        if (distance > 50f || velocity > 450f) 0f else 1f
    } else {
        if (-distance > 60f || -velocity > 450f) 1f else 0f
    }
}
