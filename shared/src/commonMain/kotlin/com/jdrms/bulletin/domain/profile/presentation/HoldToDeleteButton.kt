package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes

private fun isWithinBounds(position: Offset, size: IntSize): Boolean {
    val inHorizontalBounds = position.x in 0f..size.width.toFloat()
    val inVerticalBounds = position.y in 0f..size.height.toFloat()
    return inHorizontalBounds && inVerticalBounds
}

@Composable
internal fun HoldToDeleteButton(
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    holdDurationMillis: Int = 1500
) {
    var isPressed by remember { mutableStateOf(false) }
    var hintText by remember { mutableStateOf<String?>(null) }
    val progress = remember { Animatable(0f) }

    LaunchedEffect(isPressed) {
        if (isPressed) {
            hintText = null
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = holdDurationMillis,
                    easing = LinearEasing
                )
            )
            if (progress.value >= 1f) {
                onConfirm()
            }
        } else {
            if (progress.value in 0.01f..0.99f) {
                hintText = formatHoldHint(holdDurationMillis)
            }
            progress.snapTo(0f)
        }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .clip(BulletinShapes.pill)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                    shape = BulletinShapes.pill
                )
                .semantics {
                    role = Role.Button
                    onClick(label = "Hold to delete profile") {
                        // A semantic activation is the accessible equivalent of starting
                        // the deliberate hold. The same timed safeguard applies to every
                        // input method instead of allowing an immediate delete.
                        isPressed = true
                        true
                    }
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        isPressed = true
                        val pointerId = down.id
                        while (isPressed) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId }
                            val shouldRelease = change == null || !change.pressed ||
                                !isWithinBounds(change.position, size)
                            if (shouldRelease) {
                                isPressed = false
                                break
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            HoldProgressIndicator(
                progress = progress.value,
                modifier = Modifier.align(Alignment.CenterStart)
            )
            HoldButtonLabel(
                progress = progress.value,
                isPressed = isPressed
            )
        }

        HoldHintMessage(hintText = hintText)
    }
}
