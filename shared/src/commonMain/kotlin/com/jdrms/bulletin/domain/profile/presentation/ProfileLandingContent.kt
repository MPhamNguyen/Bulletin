package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
internal fun MarketplaceProfileView(
    uiState: ProfileUiState,
    onSettingsClick: () -> Unit,
    onEditProfileClick: () -> Unit,
    onChangePhotoClick: () -> Unit,
    onMyListingsClick: () -> Unit,
    onBookmarkedListingsClick: () -> Unit,
    onCreateListingClick: () -> Unit
) {
    val profile = uiState.profile
    val fullName = profile?.fullName?.ifBlank { null } ?: "Dominic Alfonso"
    val university = profile?.university.orEmpty()
    val major = profile?.major?.ifBlank { null } ?: "Computer Science"

    Column(modifier = Modifier.fillMaxSize()) {
        MarketplaceProfileTopBar(onSettingsClick = onSettingsClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
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
                onEditProfileClick = onEditProfileClick,
                onChangePhotoClick = onChangePhotoClick,
                avatarUrl = profile?.avatarUrl
            )

            MarketplaceActivityStatsCard(
                activeListings = uiState.activeListingsCount,
                itemsSold = uiState.itemsSoldCount,
                rating = uiState.reputation?.averageRating ?: 4.8
            )

            ProfileAboutCard(
                major = major,
                university = university,
                bio = profile?.bio,
                onEditProfileClick = onEditProfileClick
            )

            ProfileMarketplaceCard(
                activeListings = uiState.activeListingsCount,
                onMyListingsClick = onMyListingsClick,
                onBookmarkedListingsClick = onBookmarkedListingsClick,
                onCreateListingClick = onCreateListingClick
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
