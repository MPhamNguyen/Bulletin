package com.jdrms.bulletin.domain.home.presentation

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable

@Composable
internal actual fun HomePlatformTrayBackHandler(enabled: Boolean, onClose: () -> Unit) {
    BackHandler(enabled = enabled, onBack = onClose)
}
