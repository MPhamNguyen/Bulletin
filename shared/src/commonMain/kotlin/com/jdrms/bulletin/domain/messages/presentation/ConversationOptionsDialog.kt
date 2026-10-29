package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.jdrms.bulletin.core.designsystem.BulletinSpacing

internal data class ConversationOptionsActions(
    val onToggleMute: (() -> Unit)?,
    val onReportUser: () -> Unit,
    val onBlockUser: () -> Unit,
    val onDismiss: () -> Unit
)

@Composable
internal fun ConversationOptionsDialog(actions: ConversationOptionsActions) {
    Dialog(onDismissRequest = actions.onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                Modifier.padding(BulletinSpacing.xl),
                verticalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
            ) {
                Text("More options", style = MaterialTheme.typography.titleLarge)
                OptionRow(
                    Icons.Outlined.NotificationsOff,
                    "Mute notifications",
                    { actions.onToggleMute?.invoke() },
                    enabled = actions.onToggleMute != null,
                    supportingText = if (actions.onToggleMute == null) "Not available yet" else null
                )
                OptionRow(Icons.Outlined.Flag, "Report user", actions.onReportUser, tone = OptionTone.Danger)
                OptionRow(Icons.Outlined.Block, "Block user", actions.onBlockUser, tone = OptionTone.Destructive)
            }
        }
    }
}

private enum class OptionTone { Neutral, Danger, Destructive }

@Composable
private fun OptionRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    tone: OptionTone = OptionTone.Neutral,
    supportingText: String? = null
) {
    val colors = MaterialTheme.colorScheme
    val container = if (tone == OptionTone.Destructive) colors.error else colors.surfaceVariant
    val content = when (tone) {
        OptionTone.Neutral -> colors.onSurface
        OptionTone.Danger -> colors.error
        OptionTone.Destructive -> colors.onError
    }
    Surface(
        modifier = Modifier.fillMaxWidth().alpha(if (enabled) 1f else 0.5f)
            .clip(MaterialTheme.shapes.medium).clickable(enabled = enabled, role = Role.Button, onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = container,
        contentColor = content,
        border = BorderStroke(1.dp, if (tone == OptionTone.Destructive) colors.error else colors.outlineVariant)
    ) {
        Row(
            Modifier.heightIn(min = BulletinSpacing.control).padding(BulletinSpacing.m),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.m)
        ) {
            Icon(icon, null, Modifier.size(24.dp))
            Column(Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleMedium)
                supportingText?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
            }
        }
    }
}
