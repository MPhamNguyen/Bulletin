package com.jdrms.bulletin.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

private val PaperPlaneNavigationIcon = ImageVector.Builder(
    name = "PaperPlane",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(fill = SolidColor(Color.Black)) {
        moveTo(2.711f, 10.448f)
        lineTo(21.081f, 2.641f)
        curveTo(22f, 2.338f, 21.697f, 2.797f, 22f, 4.019f)
        lineTo(18.785f, 20.552f)
        curveTo(18.602f, 21.471f, 18.05f, 21.682f, 17.132f, 21.195f)
        lineTo(11.162f, 17.062f)
        curveTo(10.363f, 16.511f, 10.335f, 15.868f, 11.07f, 15.133f)
        lineTo(17.224f, 8.428f)
        curveTo(17.711f, 7.877f, 17.591f, 8.033f, 16.856f, 8.244f)
        lineTo(7.855f, 13.847f)
        curveTo(7.056f, 14.334f, 6.229f, 14.426f, 5.375f, 14.122f)
        lineTo(2.62f, 12.837f)
        curveTo(1.765f, 12.35f, 1.793f, 11.551f, 2.711f, 10.448f)
        close()
    }
}.build()

enum class AppDestination(
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon,
    val hidesOnScroll: Boolean = false
) {
    HOME("Home", Icons.Outlined.Home, Icons.Filled.Home),
    MARKETPLACE(
        "Market",
        MarketplaceNavigationIcons.outlined,
        MarketplaceNavigationIcons.filled,
        hidesOnScroll = true
    ),
    LISTINGS("Post", Icons.Default.Add),
    MESSAGES("Inbox", PaperPlaneNavigationIcon, hidesOnScroll = true),
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

data class NavigationSnapshot(
    val currentDestination: AppDestination = AppDestination.HOME,
    val profileDestination: ProfileDestination = ProfileDestination.PROFILE
) {
    val showsBottomNavigation: Boolean
        get() = currentDestination != AppDestination.PROFILE || profileDestination == ProfileDestination.PROFILE

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
