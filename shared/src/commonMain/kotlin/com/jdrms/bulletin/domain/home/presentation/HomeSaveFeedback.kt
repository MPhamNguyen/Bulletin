package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinFeedCardDefaults

private val SaveTeal = BulletinFeedCardDefaults.saveTint

@Composable
internal fun HomeSaveTint(progress: Float, modifier: Modifier = Modifier) {
    Box(
        modifier.graphicsLayer { alpha = progress }
            .background(Brush.horizontalGradient(listOf(Color.Transparent, SaveTeal.copy(alpha = 0.55f))))
    )
}

@Composable
internal fun HomeSaveStamp(progress: Float, modifier: Modifier = Modifier) {
    RowSaveStamp(progress, modifier)
}

@Composable
private fun RowSaveStamp(progress: Float, modifier: Modifier) {
    Box(
        modifier.padding(20.dp).graphicsLayer {
            alpha = (progress * 1.4f).coerceAtMost(1f)
            rotationZ = -12f
            scaleX = 0.7f + progress * 0.3f
            scaleY = scaleX
        }.clip(RoundedCornerShape(12.dp))
            .background(Color(0x66090D14))
            .padding(horizontal = 14.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text("▮ SAVE", color = SaveTeal, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
internal fun HomeSaveCelebration(modifier: Modifier = Modifier, reducedMotion: Boolean = false) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(1f, tween(950)) }
    Box(modifier, contentAlignment = Alignment.Center) {
        if (!reducedMotion) {
            androidx.compose.foundation.Canvas(Modifier.size(180.dp)) {
                val ringProgress = (progress.value / 0.684f).coerceIn(0f, 1f)
                val radius = 44.dp.toPx() * (0.4f + ringProgress * 2.4f)
                drawCircle(
                    Color(0xFF9FD8CF).copy(alpha = 0.8f * (1f - ringProgress)),
                    radius,
                    style = Stroke(3.dp.toPx())
                )
                repeat(8) { index ->
                    val dotProgress = ((progress.value - index * 0.037f) / 0.78f).coerceIn(0f, 1f)
                    val angle = index * (Math.PI / 4.0) - Math.PI / 2.0
                    val distance = 8.dp.toPx() + 58.dp.toPx() * dotProgress
                    drawCircle(
                        Color(0xFF9FD8CF).copy(alpha = (1f - dotProgress).coerceAtLeast(0f)),
                        radius = 3.5.dp.toPx(),
                        center = androidx.compose.ui.geometry.Offset(
                            size.width / 2f + kotlin.math.cos(angle).toFloat() * distance,
                            size.height / 2f + kotlin.math.sin(angle).toFloat() * distance
                        )
                    )
                }
            }
        }
        val iconProgress = (progress.value / 0.5f).coerceIn(0f, 1f)
        val iconScale = when {
            reducedMotion -> 1f
            iconProgress < 0.6f -> 0.2f + iconProgress / 0.6f * 1.15f
            else -> 1.35f - (iconProgress - 0.6f) / 0.4f * 0.35f
        }
        androidx.compose.material3.Icon(
            Icons.Filled.Bookmark,
            contentDescription = null,
            tint = SaveTeal,
            modifier = Modifier.graphicsLayer {
                alpha = if (reducedMotion) {
                    1f - progress.value
                } else {
                    1f - ((progress.value - 0.5f) / 0.5f).coerceIn(0f, 1f)
                }
                translationY = if (reducedMotion) 0f else -progress.value * 24.dp.toPx()
                scaleX = iconScale
                scaleY = iconScale
            }
        )
        if (!reducedMotion) {
            Text(
                "Saved",
                color = Color.White,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.Center).padding(top = 76.dp).graphicsLayer {
                    alpha = ((progress.value - 0.12f) / 0.72f).coerceIn(0f, 1f) *
                        (1f - ((progress.value - 0.6f) / 0.4f).coerceIn(0f, 1f))
                }
            )
        }
    }
}
