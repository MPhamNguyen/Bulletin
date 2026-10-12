package com.jdrms.bulletin.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.Home),
    MARKETPLACE("Market", Icons.Outlined.Storefront),
    LISTINGS("Post", Icons.Default.Add),
    MESSAGES("Inbox", Icons.Outlined.Mail),
    PROFILE("Profile", Icons.Outlined.AccountCircle)
}

enum class AppRootScreen {
    SIGN_IN,
    CREATE_PROFILE,
    MAIN
}

enum class ProfileDestination {
    PROFILE,
    SETTINGS,
    EDIT_ACCOUNT,
    BOOKMARKED_LISTINGS,
    NOTIFICATIONS,
    PRIVACY,
    HELP_AND_SUPPORT,
    TERMS_AND_CONDITIONS,
    PUBLIC_PROFILE,
    MY_LISTINGS,
    EDIT_LISTING
}

data class MainNavigationState(
    val currentDestination: AppDestination = AppDestination.HOME,
    val profileDestination: ProfileDestination = ProfileDestination.PROFILE
) {
    fun selectBottomNavigationDestination(destination: AppDestination): MainNavigationState = copy(
        currentDestination = destination,
        profileDestination = ProfileDestination.PROFILE
    )
}

fun backFromProfile(destination: ProfileDestination): ProfileDestination {
    return when (destination) {
        ProfileDestination.PROFILE -> ProfileDestination.PROFILE
        ProfileDestination.SETTINGS -> ProfileDestination.PROFILE
        ProfileDestination.EDIT_ACCOUNT -> ProfileDestination.PROFILE
        ProfileDestination.BOOKMARKED_LISTINGS -> ProfileDestination.PROFILE
        ProfileDestination.NOTIFICATIONS,
        ProfileDestination.PRIVACY,
        ProfileDestination.HELP_AND_SUPPORT,
        ProfileDestination.TERMS_AND_CONDITIONS,
        ProfileDestination.PUBLIC_PROFILE -> ProfileDestination.SETTINGS
        ProfileDestination.MY_LISTINGS -> ProfileDestination.PROFILE
        ProfileDestination.EDIT_LISTING -> ProfileDestination.MY_LISTINGS
    }
}

fun explicitProfileBack(destination: ProfileDestination): ProfileDestination = backFromProfile(destination)

fun systemProfileBack(destination: ProfileDestination): ProfileDestination = backFromProfile(destination)

fun backFromProfileDestination(
    destination: ProfileDestination,
    editReturnDestination: ProfileDestination
): ProfileDestination = if (destination == ProfileDestination.EDIT_ACCOUNT) {
    editReturnDestination
} else {
    backFromProfile(destination)
}
