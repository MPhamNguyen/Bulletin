package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker

/** Callbacks are UI-local; [interaction] must return the latest VM snapshot without restarting pointer input. */
internal class HomeGestureSession(
    val interaction: () -> HomeInteractionState,
    val onBegin: () -> Unit,
    val onAxis: (CardGestureResolver.Axis) -> Unit,
    val onDrag: (Float) -> Unit,
    val onRelease: (CardGestureResolver.Release) -> Unit,
    val onCancel: () -> Unit
)

/** Attach to a wrapper OUTSIDE the card's graphicsLayer. Children retain taps and consumed scrolls. */
internal fun Modifier.homeCardGestures(session: HomeGestureSession): Modifier = pointerInput(session) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        if (!CardStateMachine.acceptsPointerInput(session.interaction().card)) return@awaitEachGesture
        val isOpen = session.interaction().card == CardState.Open
        session.onBegin()
        var completed = false
        try {
            val sample = readHomeCardDrag(down, session, density, viewConfiguration.touchSlop, isOpen)
            if (sample.cancelled) session.onCancel() else session.onRelease(sample.resolve(isOpen))
            completed = true
        } finally {
            if (!completed) session.onCancel()
        }
    }
}

private class HomePointerDrag(val down: PointerInputChange, val density: Float, val isOpen: Boolean) {
    val tracker = VelocityTracker().apply { addPosition(down.uptimeMillis, down.position) }
    var axis: CardGestureResolver.Axis? = null
    var position = down.position

    fun sample(cancelled: Boolean = false): HomeGestureSample {
        val velocity = tracker.calculateVelocity()
        return homeGestureSample(
            position.x - down.position.x,
            position.y - down.position.y,
            velocity.x,
            velocity.y,
            density
        ).copy(axis = axis, cancelled = cancelled)
    }

    fun move(change: PointerInputChange, session: HomeGestureSession, slopPx: Float) {
        position = change.position
        tracker.addPosition(change.uptimeMillis, position)
        if (!change.pressed) return
        val sample = sample()
        if (axis == null) {
            axis = CardGestureResolver.resolveAxis(sample.dxDp, sample.dyDp, slopPx / density, isOpen)
            axis?.let(session.onAxis)
        }
        if (axis != null) {
            change.consume()
            if (axis == CardGestureResolver.Axis.X) session.onDrag(sample.dxDp)
        }
    }
}

private suspend fun AwaitPointerEventScope.readHomeCardDrag(
    down: PointerInputChange,
    session: HomeGestureSession,
    density: Float,
    slopPx: Float,
    isOpen: Boolean
): HomeGestureSample {
    val drag = HomePointerDrag(down, density, isOpen)
    while (true) {
        // Main pass reaches a parent after its children, so a consuming scroll keeps ownership.
        val event = awaitPointerEvent()
        val change = event.changes.firstOrNull { it.id == down.id } ?: return drag.sample(cancelled = true)
        if (event.changes.count { it.pressed } > 1 || change.isConsumed) {
            return drag.sample(cancelled = true)
        }
        drag.move(change, session, slopPx)
        if (!change.pressed) {
            if (drag.axis != null) change.consume()
            return drag.sample()
        }
    }
}

private fun HomeGestureSample.resolve(isOpen: Boolean): CardGestureResolver.Release =
    CardGestureResolver.resolveRelease(axis, dxDp, dyDp, vxDpPerMs, vyDpPerMs, isOpen, tapOnMedia = true)
