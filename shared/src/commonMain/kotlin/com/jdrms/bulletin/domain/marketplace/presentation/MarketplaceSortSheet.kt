package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceSortOrder

/** Sort choices apply as soon as one is tapped; there is no confirm step. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MarketplaceSortSheet(
    selected: MarketplaceSortOrder,
    onSelected: (MarketplaceSortOrder) -> Unit,
    onDismiss: () -> Unit
) {
    MarketplaceBottomSheet(onDismiss = onDismiss) {
        MarketplaceSheetHeader(title = "Sort by", onClose = onDismiss)
        Column(modifier = Modifier.fillMaxWidth().selectableGroup().padding(bottom = 20.dp)) {
            MarketplaceSortOrder.entries.forEach { order ->
                MarketplaceSortRow(order, order == selected) { onSelected(order) }
            }
        }
    }
}

@Composable
private fun MarketplaceSortRow(order: MarketplaceSortOrder, selected: Boolean, onClick: () -> Unit) {
    val color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = order.displayName(),
            style = if (selected) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            color = color
        )
        if (selected) {
            Icon(Icons.Outlined.Check, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        }
    }
}
