package com.jdrms.bulletin.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mail
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val hidesOnScroll: Boolean = false
) {
    HOME("Home", Icons.Outlined.Home, Icons.Filled.Home),
    MARKETPLACE("Market", Icons.Outlined.Storefront, Icons.Filled.Storefront, hidesOnScroll = true),
    LISTINGS("Post", Icons.Default.Add),
    MESSAGES("Inbox", Icons.Outlined.Mail, Icons.Filled.Mail),
    PROFILE("Profile", Icons.Outlined.AccountCircle, Icons.Filled.AccountCircle)
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
    BLOCKED_USERS,
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
    val showsBottomNavigation: Boolean
        get() = currentDestination != AppDestination.PROFILE || profileDestination == ProfileDestination.PROFILE

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
        ProfileDestination.BLOCKED_USERS -> ProfileDestination.SETTINGS
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
