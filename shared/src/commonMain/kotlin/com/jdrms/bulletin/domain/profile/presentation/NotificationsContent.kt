package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.app.notifications.LocalNotificationPermissionStatus
import com.jdrms.bulletin.app.notifications.LocalNotificationPreferences
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

@Composable
internal fun NotificationsView(
    onBack: () -> Unit
) {
    val preferences = LocalNotificationPreferences.current
    val permission = LocalNotificationPermissionStatus.current
    val enabled by preferences.enabled.collectAsState()
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        NotificationsTopBar(onBackClick = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = BulletinSpacing.l),
            verticalArrangement = Arrangement.spacedBy(BulletinSpacing.l),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            NotificationToggleCard(enabled = enabled, onEnabledChanged = preferences::setEnabled)
            Text(
                text = "Applies to all accounts on this device. In-app alerts stay on.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (!permission.allowed) {
                NotificationPermissionCard(onOpenSettings = permission.openSettings)
            }
        }
    }
}

@Composable
private fun NotificationsTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = BulletinSpacing.l, vertical = BulletinSpacing.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(40.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back to Settings",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Spacer(modifier = Modifier.width(BulletinSpacing.s))
        Text(
            text = "Notifications",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}
