package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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
    val pullRefreshState = rememberPullToRefreshState()

    Column(modifier = Modifier.fillMaxSize()) {
        MarketplaceProfileTopBar(onSettingsClick = onSettingsClick)
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            state = pullRefreshState,
            indicator = {
                ProfilePullRefreshIndicator(
                    state = pullRefreshState,
                    isRefreshing = uiState.isRefreshing,
                    isComplete = uiState.profileRefreshComplete,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            },
            modifier = Modifier.weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .offset(y = (pullRefreshState.distanceFraction.coerceIn(0f, 1f) * CONTENT_PULL_OFFSET).dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                ProfileHero(
                    fullName = fullName,
                    major = major,
                    university = university,
                    isVerified = profile?.isVerified == true || profile?.email?.isUniversityEmail == true,
                    avatarUrl = profile?.avatarUrl
                )

                MarketplaceActivityStatsCard(
                    activeListings = uiState.activeListingsCount,
                    itemsSold = uiState.itemsSoldCount,
                    rating = uiState.reputation
                        ?.takeIf { it.totalReviews > 0 }
                        ?.averageRating
                )

                ProfileAboutCard(
                    major = major,
                    university = university,
                    bio = profile?.bio
                )

                ProfileMarketplaceCard(
                    activeListings = uiState.activeListingsCount,
                    onMyListingsClick = onMyListingsClick,
                    onBookmarkedListingsClick = onBookmarkedListingsClick
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private const val CONTENT_PULL_OFFSET = 48
