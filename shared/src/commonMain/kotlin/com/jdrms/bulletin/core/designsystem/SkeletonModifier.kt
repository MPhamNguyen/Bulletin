package com.jdrms.bulletin.core.designsystem

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.jdrms.bulletin.core.designsystem.theme.BulletinMotion

/**
 * Replaces drawing inside the measured bounds and hides placeholder semantics. Does not change
 * measurement or disable input: callers must disable loading controls and their side effects.
 * Apply outside content drawing modifiers (e.g. before background) to mask the entire element.
 */
@Composable
fun Modifier.skeleton(
    visible: Boolean,
    shape: Shape = MaterialTheme.shapes.small,
    color: Color = MaterialTheme.colorScheme.surfaceVariant
): Modifier = if (visible) {
    clearAndSetSemantics { }.clip(shape).shimmerDrawing(color, replaceContent = true)
} else {
    this
}

/** Decorative overlay retained for callers that intentionally keep their content visible. */
@Composable
fun Modifier.bulletinShimmer(): Modifier =
    shimmerDrawing(MaterialTheme.colorScheme.surfaceVariant, replaceContent = false)

@Composable
private fun Modifier.shimmerDrawing(base: Color, replaceContent: Boolean): Modifier {
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
        if (replaceContent) drawRect(base) else drawContent()
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
