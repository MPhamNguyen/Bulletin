package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
fun HomeScreen(viewModel: HomeViewModel, modifier: Modifier = Modifier) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.currentIndex, state.feed.size, state.hasMore, state.nextCursor) {
        if (state.prefetchEnabled && shouldPrefetchHomeFeedPage(state.currentIndex, state.feed.size, state.hasMore)) {
            viewModel.loadNextPage()
        }
    }
    val error = state.errorMessage
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        HomeDeckContent(state, viewModel, constraints.maxWidth.toFloat())
        if (state.feed.getOrNull(state.currentIndex) != null && error != null) {
            NextPageError(
                message = error,
                isRetrying = state.isLoadingMore,
                onRetry = viewModel::loadNextPage,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            )
        }
    }
}

@Composable
private fun HomeDeckContent(state: HomeUiState, viewModel: HomeViewModel, width: Float) {
    when {
        state.feed.getOrNull(state.currentIndex) != null -> HomeCardStack(state, width) {
            viewModel.advanceCard(state.currentIndex, state.deckGeneration)
        }
        state.isDeckLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        state.errorMessage != null -> FeedErrorState(
            title = if (state.nextCursor == null) "Couldn't load listings" else "Couldn't load more listings",
            message = state.errorMessage,
            onRetry = if (state.nextCursor == null) viewModel::loadFeed else viewModel::loadNextPage,
            modifier = Modifier.fillMaxSize()
        )
        else -> EmptyFeedState(viewModel::replayLoadedCards, viewModel::refresh, Modifier.fillMaxSize())
    }
}

@Composable
private fun HomeCardStack(state: HomeUiState, width: Float, onAdvance: () -> Unit) {
    val index = state.currentIndex
    val offset = remember(index, state.deckGeneration) { Animatable(0f) }
    val current = state.feed[index]
    val next = state.feed.getOrNull(index + 1)
    // Keep the next card mounted underneath so it is revealed during the swipe.
    if (next != null) {
        HomeFeedCard(item = next, cardIndex = index + 1, isActive = false, modifier = Modifier.fillMaxSize())
    }
    HomeFeedCard(
        item = current,
        cardIndex = index,
        modifier = Modifier.fillMaxSize().graphicsLayer {
            translationX = offset.value
            rotationZ = (offset.value / width * 15f).coerceIn(-20f, 20f)
            alpha = (1f - (abs(offset.value) / width).coerceIn(0f, 1f) * 0.4f).coerceIn(0.6f, 1f)
        }.then(rememberHomeCardDrag(offset, width, onAdvance))
    )
}

@Composable
private fun rememberHomeCardDrag(
    offset: Animatable<Float, AnimationVector1D>,
    width: Float,
    onAdvance: () -> Unit
): Modifier {
    val scope = rememberCoroutineScope()
    val threshold = with(LocalDensity.current) { 100.dp.toPx() }
    return Modifier.pointerInput(offset) {
        detectHorizontalDragGestures(
            onDragEnd = { scope.launch { settleHomeCard(offset, width, threshold, onAdvance) } },
            onDragCancel = { scope.launch { offset.animateTo(0f, animationSpec = spring()) } },
            onHorizontalDrag = { change, amount ->
                change.consume()
                scope.launch { offset.snapTo(offset.value + amount) }
            }
        )
    }
}

private suspend fun settleHomeCard(
    offset: Animatable<Float, AnimationVector1D>,
    width: Float,
    threshold: Float,
    onAdvance: () -> Unit
) {
    if (abs(offset.value) > threshold) {
        val target = if (offset.value < 0f) -width * 1.3f else width * 1.3f
        offset.animateTo(target, animationSpec = tween(durationMillis = 200))
        // Advance only after the current card has fully left the viewport.
        onAdvance()
    } else {
        offset.animateTo(
            0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        )
    }
}

@Composable
private fun FeedErrorState(title: String, message: String, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.background(MaterialTheme.colorScheme.background).padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            FeedStateMessage(title, message, isError = true)
            Button(onClick = onRetry, shape = MaterialTheme.shapes.medium) {
                Text("Try again", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun FeedStateMessage(title: String, message: String, isError: Boolean = false) {
    Text(
        text = title,
        style = MaterialTheme.typography.headlineMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        textAlign = TextAlign.Center
    )
    Text(
        text = message,
        style = MaterialTheme.typography.bodyLarge,
        color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun NextPageError(
    message: String,
    isRetrying: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = onRetry, enabled = !isRetrying) {
                if (isRetrying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Retry")
                }
            }
        }
    }
}

internal fun shouldPrefetchHomeFeedPage(currentIndex: Int, feedSize: Int, hasMore: Boolean): Boolean {
    return hasMore && feedSize > 0 && feedSize - 1 - currentIndex <= PREFETCH_REMAINING_ITEMS
}

@Composable
private fun EmptyFeedState(onReset: () -> Unit, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier.background(MaterialTheme.colorScheme.background).padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "✨", style = MaterialTheme.typography.displayMedium)
            FeedStateMessage(
                title = "You're All Caught Up",
                message = "You've swiped through all available listings. Check back later for new items."
            )
            TextButton(onClick = onRefresh) { Text("Refresh") }
            Button(onClick = onReset, shape = MaterialTheme.shapes.medium) {
                Text(
                    text = "Start Over",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
