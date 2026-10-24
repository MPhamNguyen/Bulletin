package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

private const val PREFETCH_REMAINING_ITEMS = 2

@Composable
fun HomeScreen(viewModel: HomeViewModel, modifier: Modifier = Modifier, motionDurationScale: Float = 1f) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.currentIndex, state.feed.size, state.hasMore, state.nextCursor) {
        if (state.prefetchEnabled && shouldPrefetchHomeFeedPage(state.currentIndex, state.feed.size, state.hasMore)) {
            viewModel.loadNextPage()
        }
    }
    val error = state.errorMessage
    Box(modifier = modifier.fillMaxSize()) {
        HomeDeckContent(state, viewModel, motionDurationScale)
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
private fun HomeDeckContent(state: HomeUiState, viewModel: HomeViewModel, durationScale: Float) {
    when {
        state.feed.getOrNull(state.currentIndex) != null -> HomeCardStack(state, viewModel, durationScale)
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
