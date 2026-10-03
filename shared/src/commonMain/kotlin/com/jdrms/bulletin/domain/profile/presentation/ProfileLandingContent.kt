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
    onMyListingsClick: () -> Unit,
    onBookmarkedListingsClick: () -> Unit
) {
    val profile = uiState.profile
    Column(modifier = Modifier.fillMaxSize()) {
        MarketplaceProfileTopBar(onSettingsClick = onSettingsClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ProfileProminentAvatar(
                fullName = profile?.fullName?.ifBlank { null } ?: "Dominic Alfonso",
                university = profile?.university?.ifBlank { null } ?: "CSU Long Beach",
                isVerified = profile?.isVerified == true || profile?.email?.isUniversityEmail == true
            )

            ProfileAcademicInformationCard(
                major = profile?.major?.ifBlank { null } ?: "Computer Science",
                graduationDate = profile?.graduationDate?.ifBlank { null } ?: "Class of 2025"
            )

            MarketplaceActivityStatsCard(
                activeListings = uiState.activeListingsCount,
                itemsSold = uiState.itemsSoldCount,
                rating = uiState.reputation?.averageRating ?: 4.8
            )

            ProfileAboutCard(bio = profile?.bio)

            ProfileNavigationButtons(
                onMyListingsClick = onMyListingsClick,
                onBookmarkedListingsClick = onBookmarkedListingsClick
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
