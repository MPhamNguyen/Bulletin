package com.jdrms.bulletin.domain.profile.presentation

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
import com.jdrms.bulletin.app.theme.ThemePreference

@Composable
internal fun ProfileLandingView(
    uiState: ProfileUiState,
    onEditProfileClick: () -> Unit,
    onMyListingsClick: () -> Unit,
    themePreference: ThemePreference?,
    onThemePreferenceChanged: ((ThemePreference) -> Unit)?,
    onSignOut: () -> Unit
) {
    val profile = uiState.profile
    Column(modifier = Modifier.fillMaxSize()) {
        ProfileLandingTopBar(onEditProfileClick)
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(start = 20.dp, top = 2.dp, end = 20.dp, bottom = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ProfileAvatarHeader(
                fullName = profile?.fullName ?: "Student Profile",
                university = profile?.university ?: "CSU Long Beach",
                isVerified = profile?.isVerified == true || profile?.email?.isUniversityEmail == true
            )
            Spacer(modifier = Modifier.height(16.dp))
            ProfileAboutCard(profile?.bio)
            Spacer(modifier = Modifier.height(16.dp))
            ProfileAcademicInformationCard(profile?.university, profile?.major, profile?.email?.value)
            Spacer(modifier = Modifier.height(16.dp))
            ProfileReputationCard(uiState.reputation)
            Spacer(modifier = Modifier.height(16.dp))
            ProfileActionRow(ProfileIcons.MyListings, "My Listings", onMyListingsClick)
            if (themePreference != null && onThemePreferenceChanged != null) {
                Spacer(modifier = Modifier.height(16.dp))
                AppearanceCard(themePreference, onThemePreferenceChanged)
            }
            Spacer(modifier = Modifier.height(24.dp))
            SignOutButton(onSignOut)
        }
    }
}
