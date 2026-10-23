package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.domain.home.presentation.quickmessage.QUICK_MESSAGE_TRAY_HEIGHT
import kotlinx.coroutines.launch
import kotlin.math.abs

private const val PREFETCH_REMAINING_ITEMS = 2

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    onSendQuickMessage: suspend (com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem, String) -> Boolean = { _, _ -> false }
) {
    val uiState by viewModel.uiState.collectAsState()
    val items = uiState.feed
    val errorMessage = uiState.errorMessage
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
        val screenHeightPx = constraints.maxHeight.toFloat()
        val thresholdPx = with(density) { 100.dp.toPx() }

        val offsetX = remember(currentIndex) { Animatable(0f) }
        // Shared by both mounted cards so vertical gestures move the stack as one surface.
        val offsetY = remember(currentIndex) { Animatable(0f) }
        val quickMessageProgress = remember(currentIndex) { Animatable(0f) }
        val exitProgress = remember(currentIndex) { Animatable(0f) }
        val nextProgress = remember(currentIndex) { Animatable(0f) }
        var isMessageBusy by remember(currentIndex) { mutableStateOf(false) }
        val stackTranslationY = offsetY.value

        if (currentItem != null) {
            // Keep the next card mounted underneath so it is revealed during the swipe.
            if (nextItem != null) {
                HomeFeedCard(
                    item = nextItem,
                    cardIndex = currentIndex + 1,
                    isActive = false,
                    modifier = Modifier.fillMaxSize().graphicsLayer {
                        val progress = nextProgress.value
                        translationY = stackTranslationY + with(density) { (-12.dp + 12.dp * progress).toPx() }
                        scaleX = 0.94f + 0.06f * progress
                        scaleY = 0.94f + 0.06f * progress
                        alpha = 0.7f + 0.3f * progress
                    }
                )
            }

            val dragProgress = (abs(offsetX.value) / screenWidthPx).coerceIn(0f, 1f)
            val rotationAngle = (offsetX.value / screenWidthPx * 15f).coerceIn(-20f, 20f)
            val cardAlpha = (1f - dragProgress * 0.4f).coerceIn(0.6f, 1f)

            HomeFeedCard(
                item = currentItem,
                cardIndex = currentIndex,
                expansionProgress = quickMessageProgress.value,
                onSendQuickMessage = { content -> onSendQuickMessage(currentItem, content) },
                onMessageSending = { isMessageBusy = true },
                onMessageSendFailed = { isMessageBusy = false },
                onMessageAnimationComplete = {
                    coroutineScope.launch {
                        val easing = CubicBezierEasing(0.5f, 0f, 0.85f, 0.45f)
                        val exit = launch {
                            exitProgress.animateTo(1f, tween(600, easing = easing))
                        }
                        val reveal = launch {
                            nextProgress.animateTo(1f, tween(600, easing = easing))
                        }
                        exit.join()
                        reveal.join()
                        currentIndex++
                        isMessageBusy = false
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = offsetX.value
                        translationY = stackTranslationY - exitProgress.value * screenHeightPx * 1.18f
                        rotationZ = rotationAngle - exitProgress.value * 5f
                        alpha = cardAlpha * (1f - exitProgress.value)
                        transformOrigin = TransformOrigin(0.5f, 0.9f)
                    }
                    .pointerInput(currentIndex, isMessageBusy) {
                        detectHorizontalDragGestures(
                            onDragEnd = {
                                if (!isMessageBusy) coroutineScope.launch {
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
                                if (!isMessageBusy) coroutineScope.launch {
                                    offsetX.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring()
                                    )
                                }
                            },
                            onHorizontalDrag = { change, dragAmount ->
                                if (isMessageBusy) return@detectHorizontalDragGestures
                                change.consume()
                                coroutineScope.launch {
                                    offsetX.snapTo(offsetX.value + dragAmount)
                                }
                            }
                        )
                    }
                    .pointerInput(currentIndex, isMessageBusy) {
                        var dragStartProgress = 0f
                        var dragDistance = 0f
                        var dragVelocity = 0f
                        detectVerticalDragGestures(
                            onDragStart = {
                                dragStartProgress = quickMessageProgress.value
                                dragDistance = 0f
                                dragVelocity = 0f
                            },
                            onVerticalDrag = { change, dragAmount ->
                                if (isMessageBusy) return@detectVerticalDragGestures
                                change.consume()
                                dragDistance += dragAmount
                                dragVelocity = dragAmount
                                coroutineScope.launch {
                                    quickMessageProgress.snapTo(
                                        (quickMessageProgress.value - dragAmount / with(density) {
                                            QUICK_MESSAGE_TRAY_HEIGHT.toPx()
                                        })
                                            .coerceIn(0f, 1f)
                                    )
                                }
                            },
                            onDragEnd = {
                                if (isMessageBusy) return@detectVerticalDragGestures
                                val shouldOpen = if (dragStartProgress > 0.5f) {
                                    dragDistance <= 50f && dragVelocity <= 8f
                                } else {
                                    dragDistance < -60f || dragVelocity < -8f
                                }
                                coroutineScope.launch {
                                    quickMessageProgress.animateTo(
                                        if (shouldOpen) 1f else 0f,
                                        animationSpec = tween(durationMillis = 350)
                                    )
                                }
                            },
                            onDragCancel = {
                                if (!isMessageBusy) {
                                    coroutineScope.launch { quickMessageProgress.animateTo(0f, spring()) }
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
        } else if (errorMessage != null) {
            FeedErrorState(
                title = if (items.isEmpty()) "Couldn't load listings" else "Couldn't load more listings",
                message = errorMessage,
                onRetry = if (items.isEmpty()) viewModel::loadFeed else viewModel::loadNextPage,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            // The feed intentionally stops here instead of looping back through the items.
            EmptyFeedState(
                onReset = { currentIndex = 0 },
                modifier = Modifier.fillMaxSize()
            )
        }

        if (currentItem != null && errorMessage != null) {
            NextPageError(
                message = errorMessage,
                isRetrying = uiState.isLoadingMore,
                onRetry = viewModel::loadNextPage,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            )
        }
    }
}

internal fun shouldPrefetchHomeFeedPage(currentIndex: Int, feedSize: Int, hasMore: Boolean): Boolean {
    return hasMore && feedSize > 0 && feedSize - 1 - currentIndex <= PREFETCH_REMAINING_ITEMS
}
