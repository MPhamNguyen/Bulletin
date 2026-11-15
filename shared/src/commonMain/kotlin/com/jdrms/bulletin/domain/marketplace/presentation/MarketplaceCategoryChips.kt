package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory

/** "All" plus one chip per category, scrolling sideways when they do not fit. */
@Composable
internal fun MarketplaceCategoryChips(
    selected: MarketplaceCategory?,
    onSelected: (MarketplaceCategory?) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        (listOf<MarketplaceCategory?>(null) + MarketplaceCategory.entries).forEach { category ->
            MarketplaceChip(
                label = category?.displayName() ?: "All",
                selected = category == selected,
                onClick = { onSelected(category) }
            )
        }
    }
}
