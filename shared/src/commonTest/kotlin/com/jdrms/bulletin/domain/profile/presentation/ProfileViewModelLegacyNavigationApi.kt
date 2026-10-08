package com.jdrms.bulletin.domain.profile.presentation

import com.jdrms.bulletin.domain.profile.domain.model.UserId

@Deprecated("Use ProfileViewModel.actions")
fun ProfileViewModel.openSettings() = actions.navigation.openSettings()

@Deprecated("Use ProfileViewModel.actions")
fun ProfileViewModel.openProfile() = actions.navigation.openProfile()

@Deprecated("Use ProfileViewModel.actions")
fun ProfileViewModel.closeSettings() = actions.navigation.openProfile()

@Deprecated(
    "Use ProfileViewModel.actions"
)
fun ProfileViewModel.openBookmarkedListings() = actions.navigation.openBookmarkedListings()

@Deprecated(
    "Use ProfileViewModel.actions"
)
fun ProfileViewModel.closeBookmarkedListings() = actions.navigation.openProfile()

@Deprecated(
    "Use ProfileViewModel.actions"
)
fun ProfileViewModel.openNotifications() = actions.navigation.openNotifications()

@Deprecated(
    "Use ProfileViewModel.actions"
)
fun ProfileViewModel.closeNotifications() = actions.navigation.openSettings()

@Deprecated("Use ProfileViewModel.actions")
fun ProfileViewModel.openPrivacy() = actions.navigation.openPrivacy()

@Deprecated("Use ProfileViewModel.actions")
fun ProfileViewModel.closePrivacy() = actions.navigation.openSettings()

@Deprecated(
    "Use ProfileViewModel.actions"
)
fun ProfileViewModel.openHelpAndSupport() = actions.navigation.openHelpAndSupport()

@Deprecated(
    "Use ProfileViewModel.actions"
)
fun ProfileViewModel.closeHelpAndSupport() = actions.navigation.openSettings()

@Deprecated(
    "Use ProfileViewModel.actions"
)
fun ProfileViewModel.openTermsAndConditions() = actions.navigation.openTermsAndConditions()

@Deprecated(
    "Use ProfileViewModel.actions"
)
fun ProfileViewModel.closeTermsAndConditions() = actions.navigation.openSettings()

@Deprecated(
    "Use ProfileViewModel.actions"
)
fun ProfileViewModel.openPublicProfile() = actions.navigation.openPublicProfile()

@Deprecated(
    "Use ProfileViewModel.actions"
)
fun ProfileViewModel.closePublicProfile() = actions.navigation.openSettings()

@Deprecated(
    "Use ProfileViewModel.actions"
)
fun ProfileViewModel.openEditAccount() = actions.navigation.startEditingProfile()

@Deprecated(
    "Use ProfileViewModel.actions"
)
fun ProfileViewModel.closeEditAccount() = actions.navigation.cancelEditingProfile()

@Deprecated(
    "Use ProfileViewModel.actions"
)
fun ProfileViewModel.startEditingProfile() = actions.navigation.startEditingProfile()

@Deprecated(
    "Use ProfileViewModel.actions"
)
fun ProfileViewModel.cancelEditingProfile() = actions.navigation.cancelEditingProfile()

@Deprecated(
    "Use ProfileViewModel.actions"
)
fun ProfileViewModel.refreshActiveListings() = actions.overview.refreshActiveListings()

@Deprecated(
    "Use ProfileViewModel.actions"
)
fun ProfileViewModel.loadProfile(userId: UserId = ProfileViewModel.DEFAULT_USER_ID) = actions.overview.loadProfile(
    userId
)
