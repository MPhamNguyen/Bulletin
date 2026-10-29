package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinFeedCardDefaults

/** Fixed card surface revealed as the artwork lifts. */
internal val homeTraySurface: Color
    @Composable get() = MaterialTheme.colorScheme.surfaceContainer

private val TrayShadowElevation = 16.dp
private val TrayFade = 60.dp
private val TrayJoinOverlap = 16.dp

/** Position along the fade and remaining share of the artwork scrim. */
private val TrayFadeCurve = listOf(
    0f to 1f,
    0.15f to 0.92f,
    0.30f to 0.78f,
    0.48f to 0.58f,
    0.63f to 0.38f,
    0.78f to 0.20f,
    0.89f to 0.07f,
    1f to 0f
)

/**
 * Moves artwork inside the fixed card clip, extending its bottom edge into [trayColor].
 * The extension is hidden below the frame until the artwork lifts.
 * Reads [progress] only during graphics and draw phases.
 */
internal fun Modifier.homeTrayLift(trayColor: Color, progress: () -> Float): Modifier = this
    .graphicsLayer { translationY = -progress() * HOME_TRAY_LIFT_DP.dp.toPx() }
    .drawWithContent {
        drawContent()
        val lifted = progress()
        if (lifted > 0f) drawTrayExtension(trayColor, lifted)
    }

private fun DrawScope.drawTrayExtension(trayColor: Color, progress: Float) {
    val scrim = BulletinFeedCardDefaults.scrim
    val stops = TrayFadeCurve.map { (at, weight) -> at to lerp(trayColor, scrim, weight) }.toTypedArray()
    val fade = TrayFade.toPx()
    drawRect(
        brush = Brush.verticalGradient(colorStops = stops, startY = size.height, endY = size.height + fade),
        topLeft = Offset(0f, size.height),
        size = Size(size.width, HOME_TRAY_LIFT_DP.dp.toPx() + 2.dp.toPx())
    )
    // Bring real photos to the same scrim color at the join.
    val overlap = TrayJoinOverlap.toPx()
    drawRect(
        brush = Brush.verticalGradient(
            listOf(scrim.copy(alpha = 0f), scrim.copy(alpha = (progress * 4f).coerceAtMost(1f))),
            startY = size.height - overlap,
            endY = size.height
        ),
        topLeft = Offset(0f, size.height - overlap),
        size = Size(size.width, overlap)
    )
}

/** Draw outside the card shape; apply after padding and before the content clip. */
internal fun Modifier.homeTrayShadow(shape: Shape, progress: () -> Float): Modifier = graphicsLayer {
    shadowElevation = progress() * TrayShadowElevation.toPx()
    ambientShadowColor = BulletinFeedCardDefaults.scrim
    spotShadowColor = BulletinFeedCardDefaults.scrim
    this.shape = shape
    clip = false
}
