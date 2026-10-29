package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonDefaults
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

@Composable
internal fun BlockedConversationBar(
    displayName: String,
    isUnblocking: Boolean,
    errorMessage: String?,
    onUnblock: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxWidth()) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Row(
            Modifier.fillMaxWidth().padding(horizontal = BulletinSpacing.gutter, vertical = BulletinSpacing.s),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
        ) {
            Text(
                "You blocked $displayName. Unblock to send messages.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            Button(
                onUnblock,
                enabled = !isUnblocking,
                colors = BulletinButtonDefaults.neutralButtonColors(),
                modifier = Modifier.heightIn(min = BulletinSpacing.control)
            ) {
                Text(if (isUnblocking) "Unblocking…" else "Unblock")
            }
        }
        errorMessage?.let {
            Text(
                it,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(horizontal = BulletinSpacing.gutter, vertical = BulletinSpacing.s)
            )
        }
    }
}
