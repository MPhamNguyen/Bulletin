package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinFeedCardDefaults as CardColors

@Composable
internal fun HomeFeedArtwork(modifier: Modifier = Modifier) {
    val scrim = MaterialTheme.colorScheme.scrim
    Box(modifier = modifier) {
        FeedArtworkBase(Modifier.fillMaxSize())
        FeedArtworkBlur(radius = 8.dp, start = 0.30f, end = 0.72f)
        FeedArtworkBlur(radius = 22.dp, start = 0.48f, end = 0.77f)
        Box(
            Modifier.fillMaxSize().background(
                Brush.verticalGradient(
                    0f to scrim.copy(alpha = 0.45f),
                    0.22f to Color.Transparent,
                    0.40f to Color.Transparent,
                    1f to CardColors.scrim.copy(alpha = 0.92f)
                )
            )
        )
    }
}

@Composable
private fun FeedArtworkBlur(radius: Dp, start: Float, end: Float) {
    // Mask a full-size copy so both blur passes stay aligned with the sharp artwork.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        0f to Color.Transparent,
                        start to Color.Transparent,
                        end to Color.Black,
                        1f to Color.Black
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
    ) {
        FeedArtworkBase(
            modifier = Modifier.fillMaxSize().blur(radius, edgeTreatment = BlurredEdgeTreatment.Rectangle)
        )
    }
}

@Composable
private fun FeedArtworkBase(modifier: Modifier) {
    Canvas(modifier) {
        drawRect(
            Brush.verticalGradient(
                0f to CardColors.backgroundTop,
                0.45f to CardColors.backgroundMiddle,
                1f to CardColors.backgroundBottom
            )
        )
        drawArtworkCircle(
            topLeft = Offset(size.width * 0.32f, size.height * 0.09f),
            diameter = size.width * 1.05f,
            topColor = CardColors.upperCircleTop,
            bottomColor = CardColors.upperCircleBottom
        )
        drawArtworkCircle(
            topLeft = Offset(size.width * -0.35f, size.height * 0.37f),
            diameter = size.width * 0.99f,
            topColor = CardColors.leftCircleTop,
            bottomColor = CardColors.leftCircleBottom
        )
        drawArtworkCircle(
            topLeft = Offset(size.width * 0.28f, size.height * 0.59f),
            diameter = size.width * 0.84f,
            topColor = CardColors.lowerCircleTop,
            bottomColor = CardColors.lowerCircleBottom
        )
    }
}

private fun DrawScope.drawArtworkCircle(topLeft: Offset, diameter: Float, topColor: Color, bottomColor: Color) {
    val radius = diameter / 2f
    drawCircle(
        brush = Brush.verticalGradient(
            colors = listOf(topColor, bottomColor),
            startY = topLeft.y,
            endY = topLeft.y + diameter
        ),
        radius = radius,
        center = topLeft + Offset(radius, radius)
    )
}
