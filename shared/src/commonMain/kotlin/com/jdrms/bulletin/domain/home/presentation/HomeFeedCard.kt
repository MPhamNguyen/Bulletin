package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

@Composable
private fun FeedCardBackground(
    modifier: Modifier = Modifier,
    cardIndex: Int = 0
) {
    val gradientColors = when (cardIndex % 4) {
        0 -> listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.secondary,
            MaterialTheme.colorScheme.surfaceVariant
        )
        1 -> listOf(
            Color(0xFF1E3A8A),
            Color(0xFF3B82F6),
            Color(0xFF93C5FD)
        )
        2 -> listOf(
            Color(0xFF065F46),
            Color(0xFF10B981),
            Color(0xFFA7F3D0)
        )
        else -> listOf(
            Color(0xFF4C1D95),
            Color(0xFF7C3AED),
            Color(0xFFDDD6FE)
        )
    }

    val circleColors = when (cardIndex % 4) {
        0 -> Triple(
            Color(0xFFE0B85C).copy(alpha = 0.65f),
            Color(0xFF4ADE80).copy(alpha = 0.55f),
            Color(0xFF3E5C76).copy(alpha = 0.7f)
        )
        1 -> Triple(
            Color(0xFF60A5FA).copy(alpha = 0.65f),
            Color(0xFFFBBF24).copy(alpha = 0.55f),
            Color(0xFF1E40AF).copy(alpha = 0.7f)
        )
        2 -> Triple(
            Color(0xFF34D399).copy(alpha = 0.65f),
            Color(0xFFF472B6).copy(alpha = 0.55f),
            Color(0xFF047857).copy(alpha = 0.7f)
        )
        else -> Triple(
            Color(0xFFA78BFA).copy(alpha = 0.65f),
            Color(0xFFFCD34D).copy(alpha = 0.55f),
            Color(0xFF5B21B6).copy(alpha = 0.7f)
        )
    }

    Box(modifier = modifier) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(colors = gradientColors)
                )
        )

        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = circleColors.first,
                radius = size.minDimension * 0.45f,
                center = Offset(size.width * 0.8f, size.height * 0.3f)
            )
            drawCircle(
                color = circleColors.second,
                radius = size.minDimension * 0.55f,
                center = Offset(size.width * 0.2f, size.height * 0.7f)
            )
            drawCircle(
                color = circleColors.third,
                radius = size.minDimension * 0.45f,
                center = Offset(size.width * 0.75f, size.height * 0.85f)
            )
        }
    }
}

@Composable
fun HomeFeedCard(
    item: LoremCardItem,
    modifier: Modifier = Modifier,
    cardIndex: Int = 0
) {
    val textShadow = Shadow(
        color = Color.Black.copy(alpha = 0.65f),
        offset = Offset(0f, 2f),
        blurRadius = 6f
    )
    val fadeHeight = 72.dp

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val cardWidthPx = constraints.maxWidth
        val cardHeightPx = constraints.maxHeight

        FeedCardBackground(
            modifier = Modifier.fillMaxSize(),
            cardIndex = cardIndex
        )

        // Duplicate the background behind the text so the lower panel stays readable without
        // obscuring the sharp artwork above it. The mask feathers the blur into the artwork.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clipToBounds()
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        val f = (fadeHeight.toPx() / size.height).coerceIn(0.01f, 1f)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Transparent,
                                    f * 0.25f to Color.Black.copy(alpha = 0.16f),
                                    f * 0.50f to Color.Black.copy(alpha = 0.50f),
                                    f * 0.75f to Color.Black.copy(alpha = 0.84f),
                                    f to Color.Black,
                                    1f to Color.Black
                                )
                            ),
                            blendMode = BlendMode.DstIn
                        )
                    }
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(
                            Constraints.fixed(cardWidthPx, cardHeightPx)
                        )
                        layout(constraints.maxWidth, constraints.maxHeight) {
                            placeable.place(0, constraints.maxHeight - placeable.height)
                        }
                    }
            ) {
                FeedCardBackground(
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(radius = 18.dp, edgeTreatment = BlurredEdgeTreatment.Rectangle),
                    cardIndex = cardIndex
                )
            }

            // Match the blur mask with a scrim to prevent a visible transition at the panel edge.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .drawBehind {
                        val f = (fadeHeight.toPx() / size.height).coerceIn(0.01f, 1f)
                        drawRect(
                            brush = Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0f to Color.Transparent,
                                    f * 0.5f to Color.Black.copy(alpha = 0.06f),
                                    f to Color.Black.copy(alpha = 0.20f),
                                    1f to Color.Black.copy(alpha = 0.48f)
                                )
                            )
                        )
                    }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = fadeHeight, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.35f)
                    ) {
                        Text(
                            text = item.tag,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Text(
                        text = item.price,
                        style = MaterialTheme.typography.titleLarge.copy(shadow = textShadow),
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = item.title,
                    style = MaterialTheme.typography.headlineMedium.copy(shadow = textShadow),
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Text(
                    text = item.subtitle,
                    style = MaterialTheme.typography.titleSmall.copy(shadow = textShadow),
                    fontWeight = FontWeight.Medium,
                    color = Color.White.copy(alpha = 0.9f)
                )

                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium.copy(shadow = textShadow),
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 4
                )
            }
        }
    }
}
