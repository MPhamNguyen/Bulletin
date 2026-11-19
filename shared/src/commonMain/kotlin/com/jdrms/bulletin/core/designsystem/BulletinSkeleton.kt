package com.jdrms.bulletin.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import com.jdrms.bulletin.core.designsystem.theme.BulletinMotion

/** Placeholder block that stands in for content which is still loading. */
@Composable
fun BulletinSkeletonBlock(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.small
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .bulletinShimmer()
    )
}

/** Sweeps a soft highlight across the element, using theme colors so it works in light and dark. */
@Composable
fun Modifier.bulletinShimmer(): Modifier {
    val base = MaterialTheme.colorScheme.surfaceVariant
    val highlight = MaterialTheme.colorScheme.surface
    val progress by rememberInfiniteTransition(label = "bulletinShimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(BulletinMotion.SHIMMER, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bulletinShimmerProgress"
    )
    return drawWithContent {
        drawContent()
        val travel = size.width * SHIMMER_BAND_RATIO
        val startX = -travel + (size.width + travel) * progress
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(base.copy(alpha = 0f), highlight.copy(alpha = SHIMMER_ALPHA), base.copy(alpha = 0f)),
                start = Offset(startX, 0f),
                end = Offset(startX + travel, size.height * SHIMMER_TILT)
            )
        )
    }
}

private const val SHIMMER_BAND_RATIO = 0.6f
private const val SHIMMER_ALPHA = 0.7f
private const val SHIMMER_TILT = 0.2f
