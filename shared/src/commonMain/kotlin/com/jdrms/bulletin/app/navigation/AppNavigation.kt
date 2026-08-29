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
