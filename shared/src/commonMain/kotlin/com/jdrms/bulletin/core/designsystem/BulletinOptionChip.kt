package com.jdrms.bulletin.core.designsystem

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.jdrms.bulletin.core.designsystem.theme.BulletinOptionChipDefaults
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

/** Selectable form option with consistent geometry and native disabled interaction semantics. */
@Composable
fun BulletinOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    FilterChip(
        selected = selected,
        enabled = enabled,
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = BulletinSpacing.xs)
            )
        },
        modifier = modifier.height(BulletinSpacing.control),
        shape = BulletinShapes.inset,
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null) }
        } else {
            null
        },
        colors = BulletinOptionChipDefaults.colors(),
        border = BulletinOptionChipDefaults.border(enabled, selected)
    )
}
