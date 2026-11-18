package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

@Composable
internal fun NotificationPermissionCard(onOpenSettings: () -> Unit) {
    BulletinCard {
        Column(verticalArrangement = Arrangement.spacedBy(BulletinSpacing.s)) {
            Text(
                text = "Notifications are blocked in Android settings",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            BulletinButton(onClick = onOpenSettings, variant = BulletinButtonVariant.Outlined) {
                Text("Allow in settings")
            }
        }
    }
}
