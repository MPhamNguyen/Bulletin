package com.jdrms.bulletin.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinExtras

@Composable
internal fun BulletinPullRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
    isComplete: Boolean,
    modifier: Modifier = Modifier,
    completionContentDescription: String = "Updated"
) {
    val progress = state.distanceFraction.coerceIn(0f, 1f)
    val primary = MaterialTheme.colorScheme.primary
    val success = BulletinExtras.colors.success
    val track = MaterialTheme.colorScheme.surfaceVariant

    val showIndicator = isRefreshing || isComplete || (progress > 0f && !state.isAnimating)
    if (showIndicator) {
        val rotation by rememberInfiniteTransition().animateFloat(
            initialValue = 0f,
            targetValue = 360f,
            animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart)
        )
        Surface(
            modifier = modifier
                .offset(y = (progress * INDICATOR_MAX_DISTANCE).dp - INDICATOR_SIZE.dp)
                .size(INDICATOR_SIZE.dp)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().padding(3.dp)) {
                    val color = if (isComplete) success else primary
                    drawCircle(
                        color = track,
                        style = Stroke(width = RING_STROKE.toPx())
                    )
                    when {
                        isComplete -> drawArc(
                            color = color,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = Stroke(width = RING_STROKE.toPx(), cap = StrokeCap.Round)
                        )
                        isRefreshing -> drawArc(
                            color = color,
                            startAngle = rotation - 90f,
                            sweepAngle = SPINNER_SWEEP,
                            useCenter = false,
                            style = Stroke(width = RING_STROKE.toPx(), cap = StrokeCap.Round)
                        )
                        progress > 0f -> drawArc(
                            color = color,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = Stroke(width = RING_STROKE.toPx(), cap = StrokeCap.Round)
                        )
                    }
                }
                when {
                    isComplete -> Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = completionContentDescription,
                        tint = success,
                        modifier = Modifier.size(20.dp)
                    )
                    else -> Icon(
                        imageVector = Icons.Outlined.ArrowDownward,
                        contentDescription = if (progress >= 1f) "Release to refresh" else "Pull to refresh",
                        tint = primary,
                        modifier = Modifier.size(20.dp).rotate(if (progress >= 1f) 180f else 0f)
                    )
                }
            }
        }
    }
}

private const val INDICATOR_SIZE = 44
private const val INDICATOR_MAX_DISTANCE = 84
private const val SPINNER_SWEEP = 100f
private val RING_STROKE = 2.6.dp
