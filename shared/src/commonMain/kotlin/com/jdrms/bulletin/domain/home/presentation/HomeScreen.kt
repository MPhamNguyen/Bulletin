package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

private const val PREFETCH_REMAINING_ITEMS = 2

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier,
    motionDurationScale: Float = rememberHomeMotionDurationScale(),
    profileAnchorInWindow: Rect? = null,
    onSaveCelebration: (Float) -> Unit = {},
    photoPlaybackPaused: Boolean = false
) {
    val state by viewModel.uiState.collectAsState()
    LaunchedEffect(state.currentIndex, state.feed.size, state.hasMore, state.nextCursor) {
        if (state.prefetchEnabled && shouldPrefetchHomeFeedPage(state.currentIndex, state.feed.size, state.hasMore)) {
            viewModel.loadNextPage()
        }
    }
    val announcements = remember(viewModel) { HomeAnnouncements() }
    CompositionLocalProvider(LocalHomeAnnouncements provides announcements) {
        HomeScreenBody(
            state,
            viewModel,
            motionDurationScale,
            profileAnchorInWindow,
            onSaveCelebration,
            photoPlaybackPaused,
            modifier
        )
    }
}

@Composable
private fun HomeScreenBody(
    state: HomeUiState,
    viewModel: HomeViewModel,
    motionDurationScale: Float,
    profileAnchorInWindow: Rect?,
    onSaveCelebration: (Float) -> Unit,
    photoPlaybackPaused: Boolean,
    modifier: Modifier
) {
    HomeConfirmedSaveCelebration(
        state.interaction,
        viewModel,
        profileAnchorInWindow,
        motionDurationScale,
        onSaveCelebration
    )
    val error = state.errorMessage
    HomeKeyboardViewport(
        modifier.then(homeTrayDismissal(state.interaction.card, viewModel)),
        photoPlaybackPaused
    ) { paused ->
        HomeAnnouncementRegion(LocalHomeAnnouncements.current, state.interaction)
        HomeDeckContent(state, viewModel, motionDurationScale, profileAnchorInWindow, paused)
        HomeSaveRetrySnackbar(
            state,
            viewModel,
            Modifier.align(Alignment.BottomCenter).padding(BulletinSpacing.l).zIndex(1f)
        )
        HomeAccountNotice(
            state.interaction.accountNotice,
            Modifier.align(Alignment.BottomCenter).padding(BulletinSpacing.l)
        )
        if (state.feed.getOrNull(state.currentIndex) != null && error != null) {
            NextPageError(
                message = error,
                isRetrying = state.isLoadingMore,
                onRetry = viewModel::loadNextPage,
                modifier = Modifier.align(Alignment.BottomCenter).padding(BulletinSpacing.l)
            )
        }
    }
}

@Composable
private fun HomeAccountNotice(notice: HomeInteractionError?, modifier: Modifier) {
    notice?.let {
        Text(
            homeInteractionErrorText(it),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall,
            modifier = modifier
        )
    }
}

@Composable
private fun HomeDeckContent(
    state: HomeUiState,
    viewModel: HomeViewModel,
    durationScale: Float,
    anchor: Rect?,
    photoPlaybackPaused: Boolean
) {
    when {
        state.feed.getOrNull(state.currentIndex) != null -> HomeCardStack(
            state,
            viewModel,
            durationScale,
            anchor,
            photoPlaybackPaused
        )
        state.isDeckLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        state.errorMessage != null -> FeedErrorState(
            title = when {
                state.errorRetryAction == HomeErrorRetryAction.REPLAY -> "Couldn't replay listings"
                state.nextCursor == null -> "Couldn't load listings"
                else -> "Couldn't load more listings"
            },
            message = state.errorMessage,
            onRetry = when (state.errorRetryAction) {
                HomeErrorRetryAction.REPLAY -> viewModel::replayLoadedCards
                HomeErrorRetryAction.RELOAD_FEED ->
                    if (state.nextCursor == null) viewModel::loadFeed else viewModel::loadNextPage
            },
            modifier = Modifier.fillMaxSize()
        )
        else -> EmptyFeedState(
            onReset = viewModel::replayLoadedCards,
            onRefresh = viewModel::refresh,
            onLoadMore = if (state.hasMore) viewModel::loadNextPage else null,
            modifier = Modifier.fillMaxSize()
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
            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.l)
        ) {
            FeedStateMessage(title, message, isError = true)
            BulletinButton(onClick = onRetry, variant = BulletinButtonVariant.Primary) {
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
        Column(modifier = Modifier.padding(horizontal = BulletinSpacing.l, vertical = BulletinSpacing.s)) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            BulletinButton(onClick = onRetry, enabled = !isRetrying, variant = BulletinButtonVariant.Text) {
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
private fun EmptyFeedState(
    onReset: () -> Unit,
    onRefresh: () -> Unit,
    onLoadMore: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Box(
        modifier.background(MaterialTheme.colorScheme.background).padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.l)
        ) {
            Text(text = "✨", style = MaterialTheme.typography.displayMedium)
            FeedStateMessage(
                title = if (onLoadMore == null) "You're All Caught Up" else "More Listings Available",
                message = if (onLoadMore == null) {
                    "You've swiped through all available listings. Check back later for new items."
                } else {
                    "Continue loading to check for more listings."
                }
            )
            onLoadMore?.let { loadMore ->
                BulletinButton(onClick = loadMore, variant = BulletinButtonVariant.Primary) {
                    Text("Load More", style = MaterialTheme.typography.labelLarge)
                }
            }
            BulletinButton(onClick = onRefresh, variant = BulletinButtonVariant.Text) { Text("Refresh") }
            BulletinButton(onClick = onReset, variant = BulletinButtonVariant.Primary) {
                Text(
                    text = "Start Over",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
