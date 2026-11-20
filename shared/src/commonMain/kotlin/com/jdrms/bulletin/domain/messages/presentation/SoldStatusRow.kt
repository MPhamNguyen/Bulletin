package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinButton
import com.jdrms.bulletin.core.designsystem.BulletinButtonSize
import com.jdrms.bulletin.core.designsystem.theme.BulletinButtonVariant
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.core.designsystem.theme.BulletinStatusDefaults
import com.jdrms.bulletin.core.designsystem.theme.bulletinColors

@Composable
internal fun SoldStatusRow(label: String, isUpdating: Boolean, onUndo: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = BulletinStatusDefaults.containerColor,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, BulletinStatusDefaults.outlineColor)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = BulletinSpacing.m, vertical = BulletinSpacing.xs),
            horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.m),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Check,
                    contentDescription = null,
                    tint = MaterialTheme.bulletinColors.success,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    label,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            BulletinButton(
                onClick = onUndo,
                enabled = !isUpdating,
                variant = BulletinButtonVariant.NeutralOutlined,
                size = BulletinButtonSize.Compact,
                contentPadding = PaddingValues(horizontal = BulletinSpacing.m, vertical = BulletinSpacing.xs)
            ) {
                Text("Undo", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
