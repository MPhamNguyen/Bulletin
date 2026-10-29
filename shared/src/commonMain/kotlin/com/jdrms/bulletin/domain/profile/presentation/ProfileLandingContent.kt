package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.BulletinPullToRefreshBox
import com.jdrms.bulletin.core.designsystem.BulletinSpacing

@Composable
internal fun MarketplaceProfileView(
    uiState: ProfileUiState,
    onRefresh: () -> Unit,
    onSettingsClick: () -> Unit,
    onMyListingsClick: () -> Unit,
    onBookmarkedListingsClick: () -> Unit
) {
    val profile = uiState.profile
    val fullName = profile?.fullName?.ifBlank { null } ?: "Profile unavailable"
    val university = profile?.university.orEmpty()
    val major = profile?.major.orEmpty()
    val activeListingsDisplay = uiState.activeListingsCount
        .takeIf { uiState.hasLoadedOverview }
        ?.toString() ?: "—"
    val itemsSoldDisplay = uiState.itemsSoldCount.takeIf { uiState.hasLoadedOverview }

    Column(modifier = Modifier.fillMaxSize()) {
        MarketplaceProfileTopBar(onSettingsClick = onSettingsClick)
        BulletinPullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            isComplete = uiState.profileRefreshComplete,
            completionContentDescription = "Profile updated",
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = BulletinSpacing.l, vertical = BulletinSpacing.xs),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
            ) {
                ProfileHero(
                    fullName = fullName,
                    major = major,
                    university = university,
                    isVerified = profile?.isVerified == true,
                    avatarUrl = profile?.avatarUrl
                )

                MarketplaceActivityStatsCard(
                    activeListings = activeListingsDisplay,
                    itemsSold = itemsSoldDisplay,
                    rating = uiState.reputation
                        ?.takeIf { it.totalReviews > 0 }
                        ?.averageRating
                )

                ProfileAboutCard(
                    major = major,
                    university = university,
                    bio = profile?.bio
                )

                uiState.reputation?.let { reputation ->
                    ProfileReviewsSection(reputation)
                }

                ProfileMarketplaceCard(
                    activeListings = activeListingsDisplay,
                    onMyListingsClick = onMyListingsClick,
                    onBookmarkedListingsClick = onBookmarkedListingsClick
                )
                Spacer(modifier = Modifier.height(BulletinSpacing.l))
            }
        }
    }
}
