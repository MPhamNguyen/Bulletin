package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity

@Composable
internal fun Modifier.conversationBackSwipe(onBack: () -> Unit): Modifier {
    val currentOnBack = rememberUpdatedState(onBack)
    val density = LocalDensity.current.density
    return pointerInput(density) {
        var distance = 0f
        val tracker = VelocityTracker()
        detectHorizontalDragGestures(
            onDragStart = {
                distance = 0f
                tracker.resetTracking()
            },
            onDragEnd = {
                if (shouldReturnToInbox(distance / density, tracker.calculateVelocity().x / density)) {
                    currentOnBack.value()
                }
            },
            onDragCancel = { distance = 0f },
            onHorizontalDrag = { change, amount ->
                distance += amount
                tracker.addPosition(change.uptimeMillis, change.position)
                change.consume()
            }
        )
    }
}
