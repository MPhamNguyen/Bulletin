package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitTouchSlopOrCancellation
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.input.pointer.util.VelocityTracker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/** Positive delta scrolls towards the end (finger moving up). This is child ownership, not release resolution. */
internal fun homeDescriptionCanScroll(expanded: Boolean, delta: Float, forward: Boolean, backward: Boolean): Boolean =
    expanded && if (delta > 0f) forward else if (delta < 0f) backward else false

/** A child that consumed slop retains this gesture even when it later reaches a boundary. */
internal fun Modifier.homeDescriptionScroll(
    expanded: Boolean,
    state: ScrollState,
    fling: FlingBehavior,
    scope: CoroutineScope
): Modifier = pointerInput(expanded, state, fling) {
    var flingJob: Job? = null
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        flingJob?.cancel()
        val tracker = VelocityTracker().apply { addPosition(down.uptimeMillis, down.position) }
        val claim = awaitTouchSlopOrCancellation(down.id) { change, overSlop ->
            val axis = CardGestureResolver.resolveAxis(overSlop.x, -overSlop.y, 0f, false)
            if (axis == CardGestureResolver.Axis.Y &&
                homeDescriptionCanScroll(expanded, -overSlop.y, state.canScrollForward, state.canScrollBackward)
            ) {
                change.consume()
                state.dispatchRawDelta(-overSlop.y)
            }
        }
        if (claim != null) {
            tracker.addPosition(claim.uptimeMillis, claim.position)
            val velocity = readDescriptionScroll(claim.id, state, tracker)
            if (velocity != null) {
                flingJob = scope.launch {
                    state.scroll(MutatePriority.Default) { with(fling) { performFling(velocity) } }
                }
            }
        }
    }
}

private suspend fun androidx.compose.ui.input.pointer.AwaitPointerEventScope.readDescriptionScroll(
    id: androidx.compose.ui.input.pointer.PointerId,
    state: ScrollState,
    tracker: VelocityTracker
): Float? {
    while (true) {
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == id } ?: return null
        if (change.isConsumed || event.changes.count { it.pressed } > 1) return null
        tracker.addPosition(change.uptimeMillis, change.position)
        if (!change.pressed) {
            change.consume()
            return -tracker.calculateVelocity().y
        }
        state.dispatchRawDelta(-change.positionChange().y)
        change.consume()
    }
}
