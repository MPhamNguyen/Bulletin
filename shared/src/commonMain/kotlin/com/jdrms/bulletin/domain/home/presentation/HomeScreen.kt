package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

data class LoremCardItem(
    val id: String,
    val tag: String,
    val title: String,
    val subtitle: String,
    val description: String,
    val price: String
)

val defaultLoremItems = listOf(
    LoremCardItem(
        id = "1",
        tag = "Campus Discovery • Featured",
        title = "Lorem Ipsum Dolor Sit Amet",
        subtitle = "Consectetur adipiscing elit • Sed do eiusmod",
        description = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor " +
            "incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam.",
        price = "$49.99"
    ),
    LoremCardItem(
        id = "2",
        tag = "Marketplace • Textbooks",
        title = "Calculus: Early Transcendentals",
        subtitle = "8th Edition • James Stewart",
        description = "Barely used calculus textbook. Highlight-free with all companion access codes intact. " +
            "Great condition for upcoming term coursework.",
        price = "$35.00"
    ),
    LoremCardItem(
        id = "3",
        tag = "Housing • Sublease",
        title = "Studio Apartment Near Campus",
        subtitle = "Available Summer Term • Utilities Included",
        description = "Bright and spacious studio within walking distance to the engineering quad and library. " +
            "Fully furnished with high-speed internet.",
        price = "$750 / mo"
    ),
    LoremCardItem(
        id = "4",
        tag = "Student Services • Tutoring",
        title = "CS & Math Peer Tutoring",
        subtitle = "Algorithms, Data Structures & Linear Algebra",
        description = "Experienced upperclassman offering 1-on-1 tutoring sessions. Flexible schedule " +
            "and tailored exam preparation guides.",
        price = "$20 / hr"
    )
)

@Composable
fun HomeScreen(
    @Suppress("UnusedParameter", "unused") viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    items: List<LoremCardItem> = defaultLoremItems
) {
    var currentIndex by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val currentItem = items.getOrNull(currentIndex)
    val nextItem = items.getOrNull(currentIndex + 1)

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val thresholdPx = with(density) { 100.dp.toPx() }

        val offsetX = remember(currentIndex) { Animatable(0f) }

        if (currentItem != null) {
            // Background card (next listing in the deck)
            if (nextItem != null) {
                HomeFeedCard(
                    item = nextItem,
                    cardIndex = currentIndex + 1,
                    modifier = Modifier.fillMaxSize()
                )
            }

            // Foreground card with swipe gesture & rotation physics
            val dragProgress = (abs(offsetX.value) / screenWidthPx).coerceIn(0f, 1f)
            val rotationAngle = (offsetX.value / screenWidthPx * 15f).coerceIn(-20f, 20f)
            val cardAlpha = (1f - dragProgress * 0.4f).coerceIn(0.6f, 1f)

            HomeFeedCard(
                item = currentItem,
                cardIndex = currentIndex,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = offsetX.value
                        rotationZ = rotationAngle
                        alpha = cardAlpha
                    }
                    .pointerInput(currentIndex) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                coroutineScope.launch {
                                    if (offsetX.value < -thresholdPx) {
                                        // Swipe left -> dismiss offscreen to the left and advance
                                        offsetX.animateTo(
                                            targetValue = -screenWidthPx * 1.3f,
                                            animationSpec = tween(durationMillis = 200)
                                        )
                                        currentIndex++
                                    } else if (offsetX.value > thresholdPx) {
                                        // Swipe right -> dismiss offscreen to the right and advance
                                        offsetX.animateTo(
                                            targetValue = screenWidthPx * 1.3f,
                                            animationSpec = tween(durationMillis = 200)
                                        )
                                        currentIndex++
                                    } else {
                                        // Snap back to center
                                        offsetX.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                                stiffness = Spring.StiffnessMedium
                                            )
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                coroutineScope.launch {
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring()
                                    )
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                change.consume()
                                coroutineScope.launch {
                                    offsetX.snapTo(offsetX.value + dragAmount)
                                }
                            }
                        )
                    }
            )
        } else {
            // End of deck state (no infinite scroll)
            EmptyFeedState(
                onReset = { currentIndex = 0 },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
private fun EmptyFeedState(
    onReset: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "✨",
                style = MaterialTheme.typography.displayMedium
            )
            Text(
                text = "You're All Caught Up",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Text(
                text = "You've swiped through all available listings. Check back later for new items.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = onReset,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Start Over",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun FeedCardBackground(
    modifier: Modifier = Modifier,
    cardIndex: Int = 0,
    showLabel: Boolean = true,
    labelShadow: Shadow? = null
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

        if (showLabel) {
            Text(
                text = "Listing ${cardIndex + 1}",
                style = MaterialTheme.typography.titleMedium.let {
                    if (labelShadow != null) it.copy(shadow = labelShadow) else it
                },
                color = Color.White.copy(alpha = 0.85f),
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 32.dp)
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

        // 1. Full-device sharp background
        FeedCardBackground(
            modifier = Modifier.fillMaxSize(),
            cardIndex = cardIndex,
            showLabel = true,
            labelShadow = textShadow
        )

        // 2. Blur panel: wraps the text with smooth alpha feathering
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
                    cardIndex = cardIndex,
                    showLabel = false
                )
            }

            // Scrim: also fades in over the same zone so there's no visible dark edge
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

            // Typography content with horizontal padding inside the full-width blur panel
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
