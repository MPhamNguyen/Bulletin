package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceLayoutPreference
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceSortOrder

/** Layout toggle on the left, "Sort: …" control on the right. */
@Composable
internal fun MarketplaceToolRow(
    layout: MarketplaceLayoutPreference,
    onLayoutChanged: (MarketplaceLayoutPreference) -> Unit,
    sortOrder: MarketplaceSortOrder,
    onSortSelected: (MarketplaceSortOrder) -> Unit
) {
    var isSortSheetOpen by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MarketplaceLayoutToggle(layout, onLayoutChanged)
        MarketplaceChip(
            label = "Sort: ${sortOrder.displayName()}",
            selected = false,
            onClick = { isSortSheetOpen = true },
            trailingIcon = Icons.Outlined.KeyboardArrowDown
        )
    }
    if (isSortSheetOpen) {
        MarketplaceSortSheet(
            selected = sortOrder,
            onSelected = {
                isSortSheetOpen = false
                onSortSelected(it)
            },
            onDismiss = { isSortSheetOpen = false }
        )
    }
}
