package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceSellerProfile
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Composable
internal fun SellerAllReviewsScreen(
    profile: MarketplaceSellerProfile,
    isLoadingMoreReviews: Boolean,
    reviewLoadError: String?,
    onLoadMoreReviews: () -> Unit,
    onBack: () -> Unit,
    onListingClick: (String) -> Unit
) {
    val listState = rememberLazyListState()
    LaunchedEffect(listState, profile.reviews.size, profile.reviewNextOffset, isLoadingMoreReviews) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }
            .map { lastVisibleIndex -> lastVisibleIndex >= profile.reviews.size - 1 }
            .distinctUntilChanged()
            .collect { nearEnd ->
                if (nearEnd) {
                    val canLoadMore = profile.reviewNextOffset != null &&
                        !isLoadingMoreReviews &&
                        reviewLoadError == null
                    if (canLoadMore) onLoadMoreReviews()
                }
            }
    }
    Column(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        SellerProfileTopBar(
            onBack = onBack,
            onReportUser = {},
            onBlockUser = {},
            showUserActions = false,
            title = "All reviews",
            backContentDescription = "Back to seller profile"
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = BulletinSpacing.m),
            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
        ) {
            item {
                Text(
                    "Reviews for ${profile.name ?: "this seller"}",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            if (profile.reviews.isEmpty() && reviewLoadError != null) {
                item {
                    BulletinCard {
                        Text(reviewLoadError, style = MaterialTheme.typography.bodyMedium)
                        BulletinButton(
                            onClick = onLoadMoreReviews,
                            enabled = !isLoadingMoreReviews,
                            variant = BulletinButtonVariant.Text
                        ) {
                            Text(if (isLoadingMoreReviews) "Loading reviews…" else "Try again")
                        }
                    }
                }
            } else if (profile.reviews.isEmpty()) {
                item {
                    BulletinCard {
                        Text(
                            "This seller has no reviews yet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(profile.reviews, key = { it.id }) { review ->
                    SellerReviewCard(review, onListingClick)
                }
                if (isLoadingMoreReviews) {
                    item {
                        Text(
                            "Loading more reviews…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                reviewLoadError?.let { error ->
                    item {
                        BulletinCard {
                            Text(error, style = MaterialTheme.typography.bodyMedium)
                            BulletinButton(
                                onClick = onLoadMoreReviews,
                                variant = BulletinButtonVariant.Text
                            ) {
                                Text("Try again")
                            }
                        }
                    }
                }
            }
        }
    }
}
