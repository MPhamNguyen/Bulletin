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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.jdrms.bulletin.app.theme.ThemeViewModel
import com.jdrms.bulletin.core.designsystem.BulletinExtras

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    themeViewModel: ThemeViewModel? = null,
    onBack: () -> Unit = {},
    onSignOut: () -> Unit = {},
    onMyListingsClick: () -> Unit = {},
    onBookmarkedListingsClick: () -> Unit = {},
    onCreateListingClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.actions.overview.refreshActiveListings()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val handleBack = { handleSubscreenBack(uiState.activeSubscreen, viewModel, onBack) }

        ProfileSubscreenHost(
            uiState = uiState,
            viewModel = viewModel,
            themeViewModel = themeViewModel,
            handleBack = handleBack,
            onSignOut = onSignOut,
            landingActions = ProfileLandingActions(
                onSettings = viewModel.actions.navigation::openSettings,
                onEditProfile = viewModel.actions.navigation::startEditingProfile,
                onMyListings = onMyListingsClick,
                onBookmarkedListings = {
                    onBookmarkedListingsClick()
                    viewModel.actions.navigation.openBookmarkedListings()
                },
                onCreateListing = onCreateListingClick
            )
        )

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
private fun ProfileSubscreenHost(
    uiState: ProfileUiState,
    viewModel: ProfileViewModel,
    themeViewModel: ThemeViewModel?,
    handleBack: () -> Unit,
    onSignOut: () -> Unit,
    landingActions: ProfileLandingActions
) {
    val profilePhotoPicker = rememberProfilePhotoPicker(
        onPhotoSelected = viewModel.actions.editing::uploadPhoto,
        onError = viewModel.actions.editing::onPhotoSelectionError
    )

    when {
        uiState.activeSubscreen == ProfileSubscreen.EDIT_ACCOUNT || uiState.isEditingProfile -> {
            EditProfileView(
                uiState = uiState,
                onCancel = viewModel.actions.navigation::cancelEditingProfile,
                onDraftChanged = viewModel.actions.editing::onDraftChanged,
                onUpdate = viewModel.actions.editing::updateDetails,
                onChangePhoto = profilePhotoPicker,
                onSignOut = onSignOut
            )
        }
        uiState.activeSubscreen == ProfileSubscreen.SETTINGS -> {
            SettingsView(
                uiState = uiState,
                onBack = handleBack,
                actions = SettingsActions(
                    onEditAccount = viewModel.actions.navigation::startEditingProfile,
                    onViewPublicProfile = viewModel.actions.navigation::openPublicProfile,
                    onNotifications = viewModel.actions.navigation::openNotifications,
                    onPrivacy = viewModel.actions.navigation::openPrivacy,
                    onHelpSupport = viewModel.actions.navigation::openHelpAndSupport,
                    onTermsConditions = viewModel.actions.navigation::openTermsAndConditions,
                    onSignOut = onSignOut,
                    onConfirmDeleteProfile = {
                        viewModel.actions.account.deleteProfile(onSuccess = onSignOut)
                    }
                ),
                themePreference = themeViewModel?.themePreference?.collectAsState()?.value,
                onThemePreferenceChanged = themeViewModel?.let { vm -> vm::setThemePreference }
            )
        }
        uiState.activeSubscreen == ProfileSubscreen.BOOKMARKED_LISTINGS -> {
            BookmarkedListingsView(onBack = handleBack)
        }
        uiState.activeSubscreen == ProfileSubscreen.NOTIFICATIONS -> {
            NotificationsView(onBack = handleBack)
        }
        uiState.activeSubscreen == ProfileSubscreen.PRIVACY -> {
            PrivacyView(onBack = handleBack)
        }
        uiState.activeSubscreen == ProfileSubscreen.HELP_AND_SUPPORT -> {
            HelpAndSupportView(onBack = handleBack)
        }
        uiState.activeSubscreen == ProfileSubscreen.TERMS_AND_CONDITIONS -> {
            TermsAndConditionsView(onBack = handleBack)
        }
        uiState.activeSubscreen == ProfileSubscreen.PUBLIC_PROFILE -> {
            PublicProfileView(
                profile = uiState.profile,
                onBack = handleBack
            )
        }
        else -> {
            MarketplaceProfileView(
                uiState = uiState,
                onSettingsClick = landingActions.onSettings,
                onEditProfileClick = landingActions.onEditProfile,
                onChangePhotoClick = profilePhotoPicker,
                onMyListingsClick = landingActions.onMyListings,
                onBookmarkedListingsClick = landingActions.onBookmarkedListings,
                onCreateListingClick = landingActions.onCreateListing
            )
        }
    }
}

private data class ProfileLandingActions(
    val onSettings: () -> Unit,
    val onEditProfile: () -> Unit,
    val onMyListings: () -> Unit,
    val onBookmarkedListings: () -> Unit,
    val onCreateListing: () -> Unit
)

internal fun handleSubscreenBack(
    activeSubscreen: ProfileSubscreen,
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    when (activeSubscreen) {
        ProfileSubscreen.EDIT_ACCOUNT -> viewModel.actions.navigation.cancelEditingProfile()
        ProfileSubscreen.SETTINGS -> viewModel.actions.navigation.openProfile()
        ProfileSubscreen.BOOKMARKED_LISTINGS -> viewModel.actions.navigation.openProfile()
        ProfileSubscreen.NOTIFICATIONS -> viewModel.actions.navigation.openSettings()
        ProfileSubscreen.PRIVACY -> viewModel.actions.navigation.openSettings()
        ProfileSubscreen.HELP_AND_SUPPORT -> viewModel.actions.navigation.openSettings()
        ProfileSubscreen.TERMS_AND_CONDITIONS -> viewModel.actions.navigation.openSettings()
        ProfileSubscreen.PUBLIC_PROFILE -> viewModel.actions.navigation.openSettings()
        ProfileSubscreen.PROFILE -> onBack()
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
