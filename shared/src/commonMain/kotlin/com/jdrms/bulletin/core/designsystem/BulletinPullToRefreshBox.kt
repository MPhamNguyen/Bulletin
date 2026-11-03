package com.jdrms.bulletin.core.designsystem

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Shared pull-to-refresh container with Bulletin's progress and completion indicator.
 * Content must provide a vertical scrollable (such as a LazyColumn or verticalScroll).
 * The caller owns refreshing and completion state, including clearing completion feedback.
 */
@Composable
fun BulletinPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    isComplete: Boolean = false,
    completionContentDescription: String = "Updated",
    contentPullOffset: Dp = 48.dp,
    content: @Composable BoxScope.() -> Unit
) {
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = state,
        modifier = modifier,
        indicator = {
            BulletinPullRefreshIndicator(
                state = state,
                isRefreshing = isRefreshing,
                isComplete = isComplete,
                completionContentDescription = completionContentDescription,
                modifier = Modifier.align(Alignment.TopCenter)
            )
        }
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
                .offset(y = contentPullOffset * state.distanceFraction.coerceIn(0f, 1f)),
            content = content
        )
    }
}
