package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.jdrms.bulletin.core.designsystem.BulletinExtras

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    themeViewModel: com.jdrms.bulletin.app.theme.ThemeViewModel? = null,
    onBack: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onMyListingsClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        if (uiState.isEditingProfile) {
            EditProfileView(
                uiState = uiState,
                onBack = {
                    viewModel.cancelEditingProfile()
                    onBack()
                },
                onCancel = viewModel::cancelEditingProfile,
                onDraftChanged = viewModel::onProfileDraftChanged,
                onUpdate = viewModel::updateProfileDetails,
                onSignOut = onSignOut
            )
        } else {
            ProfileLandingView(
                uiState = uiState,
                onEditProfileClick = viewModel::startEditingProfile,
                onMyListingsClick = onMyListingsClick,
                themePreference = themeViewModel?.themePreference?.collectAsState()?.value,
                onThemePreferenceChanged = themeViewModel?.let { vm -> vm::setThemePreference },
                onSignOut = onSignOut
            )
        }

        AnimatedContent(
            targetState = uiState.successMessage,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp, start = 24.dp, end = 24.dp)
                .zIndex(1f),
            transitionSpec = {
                (slideInVertically(initialOffsetY = { -it }) + fadeIn()) togetherWith
                    (slideOutVertically(targetOffsetY = { -it }) + fadeOut())
            },
            label = "Profile flash notification"
        ) { message ->
            if (message != null) ProfileUpdateMessage(message)
        }
    }
}

@Composable
private fun ProfileUpdateMessage(message: String) {
    Row(
        modifier = Modifier
            .shadow(6.dp, MaterialTheme.shapes.large)
            .background(
                color = BulletinExtras.colors.successContainer,
                shape = MaterialTheme.shapes.large
            )
            .padding(horizontal = 24.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = BulletinExtras.colors.success
        )
        Text(
            text = message,
            style = MaterialTheme.typography.titleMedium,
            color = BulletinExtras.colors.onSuccessContainer
        )
    }
}
