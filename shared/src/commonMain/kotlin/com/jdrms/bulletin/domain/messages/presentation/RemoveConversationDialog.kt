package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinElevation
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

@Composable
internal fun RemoveConversationDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            shadowElevation = BulletinElevation.overlay
        ) {
            Column(
                modifier = Modifier.padding(BulletinSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(BulletinSpacing.l)
            ) {
                Text("Remove conversation?", style = MaterialTheme.typography.titleLarge)
                Text(
                    "Earlier messages will no longer appear for you. The other person keeps their history.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
                ) {
                    BulletinButton(
                        modifier = Modifier.weight(1f),
                        onClick = onDismiss,
                        variant = BulletinButtonVariant.Outlined
                    ) {
                        Text("Cancel")
                    }
                    BulletinButton(
                        modifier = Modifier.weight(1f),
                        onClick = onConfirm,
                        variant = BulletinButtonVariant.Destructive
                    ) {
                        Text("Remove")
                    }
                }
            }
        }
    }
}
