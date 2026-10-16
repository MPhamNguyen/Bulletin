package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val PREFETCH_REMAINING_ITEMS = 2

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val items = uiState.feed
    var currentIndex by remember { mutableStateOf(0) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    val currentItem = items.getOrNull(currentIndex)
    val nextItem = items.getOrNull(currentIndex + 1)

    LaunchedEffect(currentIndex, items.size, uiState.hasMore, uiState.nextCursor) {
        if (shouldPrefetchHomeFeedPage(currentIndex, items.size, uiState.hasMore)) {
            viewModel.loadNextPage()
        }
    }
    LaunchedEffect(uiState.isLoading) {
        if (uiState.isLoading) currentIndex = 0
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        val thresholdPx = with(density) { 100.dp.toPx() }

        val offsetX = remember(currentIndex) { Animatable(0f) }

        if (currentItem != null) {
            // Keep the next card mounted underneath so it is revealed during the swipe.
            if (nextItem != null) {
                HomeFeedCard(
                    item = nextItem,
                    cardIndex = currentIndex + 1,
                    modifier = Modifier.fillMaxSize()
                )
            }

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
                                        // Advance only after the current card has fully left the viewport.
                                        offsetX.animateTo(
                                            targetValue = -screenWidthPx * 1.3f,
                                            animationSpec = tween(durationMillis = 200)
                                        )
                                        currentIndex++
                                    } else if (offsetX.value > thresholdPx) {
                                        offsetX.animateTo(
                                            targetValue = screenWidthPx * 1.3f,
                                            animationSpec = tween(durationMillis = 200)
                                        )
                                        currentIndex++
                                    } else {
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
        } else if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            // The feed intentionally stops here instead of looping back through the items.
            EmptyFeedState(
                onReset = { currentIndex = 0 },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

internal fun shouldPrefetchHomeFeedPage(currentIndex: Int, feedSize: Int, hasMore: Boolean): Boolean {
    return hasMore && feedSize > 0 && feedSize - 1 - currentIndex <= PREFETCH_REMAINING_ITEMS
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
