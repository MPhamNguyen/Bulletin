package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.jdrms.bulletin.core.designsystem.BulletinCard
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

@Composable
internal fun NotificationToggleCard(enabled: Boolean, onEnabledChanged: (Boolean) -> Unit) {
    BulletinCard {
        Row(
            modifier = Modifier.fillMaxWidth()
                .toggleable(value = enabled, role = Role.Switch, onValueChange = onEnabledChanged)
                .padding(vertical = BulletinSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.m),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Push notifications",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Messages and matching listings",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(checked = enabled, onCheckedChange = null)
        }
    }
}
