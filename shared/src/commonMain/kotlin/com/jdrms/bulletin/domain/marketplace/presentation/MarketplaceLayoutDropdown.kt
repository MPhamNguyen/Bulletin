package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceLayoutPreference

@Composable
internal fun MarketplaceLayoutToggle(
    selected: MarketplaceLayoutPreference,
    onSelected: (MarketplaceLayoutPreference) -> Unit
) {
    val shape = MaterialTheme.shapes.medium
    Row(
        modifier = Modifier
            .selectableGroup()
            .background(MaterialTheme.colorScheme.surface, shape)
            .border(1.dp, MaterialTheme.colorScheme.outline, shape)
            .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MarketplaceLayoutOption(
            preference = MarketplaceLayoutPreference.ONE_PER_ROW,
            selected = selected == MarketplaceLayoutPreference.ONE_PER_ROW,
            onClick = { onSelected(MarketplaceLayoutPreference.ONE_PER_ROW) }
        )
        MarketplaceLayoutOption(
            preference = MarketplaceLayoutPreference.TWO_PER_ROW,
            selected = selected == MarketplaceLayoutPreference.TWO_PER_ROW,
            onClick = { onSelected(MarketplaceLayoutPreference.TWO_PER_ROW) }
        )
    }
}

@Composable
private fun MarketplaceLayoutOption(
    preference: MarketplaceLayoutPreference,
    selected: Boolean,
    onClick: () -> Unit
) {
    val shape = MaterialTheme.shapes.small
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                color = if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                shape = shape
            )
            .semantics { contentDescription = "${preference.columns} item per row" }
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        LayoutColumnsIcon(
            columns = preference.columns,
            color = if (selected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun LayoutColumnsIcon(columns: Int, color: Color) {
    if (columns == 1) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(3) { LayoutShape(18.dp, 5.dp, color) }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            repeat(2) {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    repeat(2) { LayoutShape(8.dp, 8.dp, color) }
                }
            }
        }
    }
}

@Composable
private fun LayoutShape(width: androidx.compose.ui.unit.Dp, height: androidx.compose.ui.unit.Dp, color: Color) {
    Box(
        Modifier
            .size(width, height)
            .background(color, RoundedCornerShape(1.dp))
    )
}
