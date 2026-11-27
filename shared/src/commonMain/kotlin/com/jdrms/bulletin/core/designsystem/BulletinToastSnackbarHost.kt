package com.jdrms.bulletin.core.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

/** Keeps Material's message queue, accessible timeouts and action results with Bulletin toast visuals. */
@Composable
fun BulletinToastSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
    showSuccessIcon: Boolean = true
) {
    SnackbarHost(hostState, modifier) { data ->
        BulletinToast(data.visuals.message, showSuccessIcon = showSuccessIcon) {
            ToastActions(data)
        }
    }
}

@Composable
private fun ToastActions(data: SnackbarData) {
    if (data.visuals.actionLabel == null && !data.visuals.withDismissAction) return
    Row(
        horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s),
        verticalAlignment = Alignment.CenterVertically
    ) {
        data.visuals.actionLabel?.let { label ->
            BulletinButton(
                onClick = data::performAction,
                variant = BulletinButtonVariant.Text,
                size = BulletinButtonSize.Compact
            ) {
                Text(label, color = BulletinToastDefaults.Content)
            }
        }
        if (data.visuals.withDismissAction) {
            IconButton(onClick = data::dismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss", tint = BulletinToastDefaults.Content)
            }
        }
    }
}
