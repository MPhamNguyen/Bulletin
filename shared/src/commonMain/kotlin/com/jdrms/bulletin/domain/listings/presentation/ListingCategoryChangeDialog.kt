package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinElevation

/** Asks before a category switch discards nonblank item details. Cancel leaves the form unchanged. */
@Composable
internal fun ListingCategoryChangeDialog(
    change: PendingCategoryChange,
    onConfirm: () -> Unit,
    onCancel: () -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = scheme.surface,
            contentColor = scheme.onSurface,
            tonalElevation = BulletinElevation.floating
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                WarningBadge()
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Change category?",
                        style = MaterialTheme.typography.titleLarge,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Switching to ${change.target.displayLabel()} will remove the details you entered " +
                            "for the current category. This can't be undone.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
                DiscardedDetailsList(change.discarded.map { it.label })
                CategoryChangeActions(onConfirm, onCancel)
            }
        }
    }
}

@Composable
private fun WarningBadge() {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = Modifier.size(56.dp).background(scheme.errorContainer, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.WarningAmber,
            contentDescription = null,
            tint = scheme.onErrorContainer,
            modifier = Modifier.size(28.dp)
        )
    }
}

@Composable
private fun CategoryChangeActions(onConfirm: () -> Unit, onCancel: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        BulletinButton(
            onClick = onCancel,
            modifier = Modifier.fillMaxWidth(),
            variant = BulletinButtonVariant.Primary
        ) { Text("Keep category", fontWeight = FontWeight.Bold) }
        BulletinButton(
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            variant = BulletinButtonVariant.DestructiveOutlined
        ) { Text("Change category", fontWeight = FontWeight.Bold) }
    }
}

@Composable
private fun DiscardedDetailsList(labels: List<String>) {
    val scheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth()
            .background(scheme.errorContainer, MaterialTheme.shapes.medium)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Will be removed",
            style = MaterialTheme.typography.labelMedium,
            color = scheme.onErrorContainer.copy(alpha = 0.8f)
        )
        labels.forEach { label ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null,
                    tint = scheme.onErrorContainer,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = scheme.onErrorContainer
                )
            }
        }
    }
}
