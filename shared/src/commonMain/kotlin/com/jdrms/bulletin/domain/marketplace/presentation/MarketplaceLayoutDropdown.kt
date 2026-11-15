package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinShapes
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceLayoutPreference

@Composable
internal fun MarketplaceLayoutToggle(
    selected: MarketplaceLayoutPreference,
    onSelected: (MarketplaceLayoutPreference) -> Unit
) {
    Row(
        modifier = Modifier
            .selectableGroup()
            .clip(BulletinShapes.inset)
            .border(1.dp, MaterialTheme.colorScheme.outline, BulletinShapes.inset)
    ) {
        MarketplaceLayoutPreference.entries.forEach { preference ->
            MarketplaceLayoutOption(
                icon = if (preference.columns == 1) Icons.Outlined.Menu else Icons.Outlined.GridView,
                description = "${preference.columns} item per row",
                selected = selected == preference,
                onClick = { onSelected(preference) }
            )
        }
    }
}

@Composable
private fun MarketplaceLayoutOption(
    icon: ImageVector,
    description: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .size(width = 44.dp, height = 40.dp)
            .background(if (selected) MaterialTheme.colorScheme.surfaceContainerHighest else Color.Transparent)
            .semantics { contentDescription = description }
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
    }
}
