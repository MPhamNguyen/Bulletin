package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.platformReducedMotion
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem

private const val PREFETCH_REMAINING_ITEMS = 2

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    onSendQuickMessage: suspend (HomeFeedItem, String) -> Boolean = { _, _ -> false },
    onSaveListing: (HomeFeedItem) -> Unit = {},
    saveErrorMessage: String? = null,
    onRetrySave: () -> Unit = {},
    profileTargetPosition: Offset = Offset.Zero
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(saveErrorMessage) {
        if (saveErrorMessage != null) {
            val result = snackbarHostState.showSnackbar(saveErrorMessage, actionLabel = "Retry")
            if (result == SnackbarResult.ActionPerformed) {
                onRetrySave()
            }
        }
    }
    val current = state.feed.firstOrNull()
    LaunchedEffect(state.feed.size, state.hasMore, state.nextCursor) {
        if (!state.isLoading && shouldPrefetchHomeFeedPage(0, state.feed.size, state.hasMore)) {
            viewModel.loadNextPage()
        }
    }
    Box(modifier.fillMaxSize()) {
        if (current != null) {
            HomeCurrentListing(state, viewModel, onSendQuickMessage, onSaveListing, profileTargetPosition)
        } else {
            HomeFeedStatus(state, viewModel)
        }
        if (current != null && state.errorMessage != null) {
            NextPageError(
                message = state.errorMessage.orEmpty(),
                isRetrying = state.isLoadingMore,
                onRetry = viewModel::loadNextPage,
                modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp)
            )
        }
        SnackbarHost(snackbarHostState, Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp))
    }
}

@Composable
private fun HomeCurrentListing(
    state: HomeUiState,
    viewModel: HomeViewModel,
    onSendQuickMessage: suspend (HomeFeedItem, String) -> Boolean,
    onSaveListing: (HomeFeedItem) -> Unit,
    profileTargetPosition: Offset
) {
    val current = state.feed.first()
    val generation = state.sessionGeneration
    key(generation, current.id) {
        HomeFeedStack(
            item = current,
            nextItem = state.feed.getOrNull(1),
            reducedMotion = platformReducedMotion(),
            profileTargetPosition = profileTargetPosition,
            actions = HomeFeedActions(
                onSendQuickMessage = { onSendQuickMessage(current, it) },
                onSave = { onSaveListing(current) },
                onPass = { viewModel.passListing(current.id, generation) },
                onAdvance = { viewModel.advanceListing(current.id, generation) }
            )
        )
    }
}

@Composable
private fun HomeFeedStatus(state: HomeUiState, viewModel: HomeViewModel) {
    Box(Modifier.fillMaxSize()) {
        if (state.isLoading || state.isLoadingMore) {
            CircularProgressIndicator(Modifier.align(Alignment.Center), color = MaterialTheme.colorScheme.primary)
        } else if (state.errorMessage != null) {
            FeedErrorState(
                title = "Couldn't load listings",
                message = state.errorMessage.orEmpty(),
                onRetry = if (state.hasMore) viewModel::loadNextPage else viewModel::loadFeed,
                modifier = Modifier.fillMaxSize()
            )
        } else if (!state.hasMore) {
            EmptyFeedState(onReset = viewModel::loadFeed, modifier = Modifier.fillMaxSize())
        }
    }
}

internal fun shouldPrefetchHomeFeedPage(currentIndex: Int, feedSize: Int, hasMore: Boolean): Boolean =
    hasMore && feedSize - 1 - currentIndex <= PREFETCH_REMAINING_ITEMS
