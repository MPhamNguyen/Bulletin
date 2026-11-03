package com.jdrms.bulletin.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val hidesOnScroll: Boolean = false
) {
    HOME("Home", HomeNavigationIcons.outlined, HomeNavigationIcons.filled),
    MARKETPLACE(
        "Market",
        Icons.Outlined.ShoppingBag,
        Icons.Filled.ShoppingBag,
        hidesOnScroll = true
    ),
    LISTINGS("Post", Icons.Default.Add),
    MESSAGES(
        "Inbox",
        MessagesNavigationIcons.outline,
        MessagesNavigationIcons.filled,
        hidesOnScroll = true
    ),
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
    PUBLIC_PROFILE,
    MY_LISTINGS,
    EDIT_LISTING
}

data class NavigationSnapshot(
    val currentDestination: AppDestination = AppDestination.HOME,
    val profileDestination: ProfileDestination = ProfileDestination.PROFILE
) {
    val showsBottomNavigation: Boolean
        get() = when (currentDestination) {
            AppDestination.LISTINGS -> false
            AppDestination.PROFILE -> profileDestination == ProfileDestination.PROFILE
            else -> true
        }

    fun selectBottomNavigationDestination(destination: AppDestination): NavigationSnapshot = copy(
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
