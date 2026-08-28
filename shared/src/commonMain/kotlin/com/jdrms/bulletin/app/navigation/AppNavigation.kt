package com.jdrms.bulletin.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.ui.graphics.vector.ImageVector

enum class AppDestination(val label: String, val icon: ImageVector) {
    HOME("Home", Icons.Outlined.Home),
    MARKETPLACE("Market", Icons.Outlined.Storefront),
    CREATE_LISTING("Post", Icons.Default.Add),
    MESSAGING("Inbox", Icons.AutoMirrored.Outlined.Send),
    PROFILE("Profile", Icons.Outlined.AccountCircle)
}
