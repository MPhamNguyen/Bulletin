package com.jdrms.bulletin.domain.profile.presentation

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinCard

data class ReportedListingVisibilitySetting(
    val hideReportedListings: Boolean,
    val onChanged: (Boolean) -> Unit
)

@Composable
internal fun ReportedListingVisibilityCard(setting: ReportedListingVisibilitySetting) {
    val hideReportedListings = setting.hideReportedListings
    BulletinCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .toggleable(
                    value = hideReportedListings,
                    role = Role.Switch,
                    onValueChange = setting.onChanged
                )
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            androidx.compose.foundation.layout.Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hide listings I report",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Reported listings will be removed from your marketplace and Home feeds.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = hideReportedListings,
                onCheckedChange = null
            )
        }
    }
}
