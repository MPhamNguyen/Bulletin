package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePriceRange
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceSortOrder

/** One removable chip per applied filter. */
@Composable
internal fun ActiveMarketplaceFilterChips(
    filters: MarketplaceFilterState,
    includeCategoryAndSort: Boolean,
    onFiltersApplied: (MarketplaceFilterState) -> Unit
) {
    if (filters.activeFilterCount(includeCategoryAndSort) == 0) return
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (includeCategoryAndSort) {
            filters.category?.let { category ->
                RemovableFilterChip(category.displayName()) { onFiltersApplied(filters.copy(category = null)) }
            }
        }
        filters.condition?.let { condition ->
            RemovableFilterChip(condition.displayName()) { onFiltersApplied(filters.copy(condition = null)) }
        }
        if (!filters.priceRange.isEmpty) {
            RemovableFilterChip(filters.priceRange.displayName()) {
                onFiltersApplied(filters.copy(priceRange = MarketplacePriceRange.NONE))
            }
        }
        if (includeCategoryAndSort && filters.sortOrder != MarketplaceSortOrder.NEWEST) {
            RemovableFilterChip(filters.sortOrder.displayName()) {
                onFiltersApplied(filters.copy(sortOrder = MarketplaceSortOrder.NEWEST))
            }
        }
        ActiveAttributeFilterChips(filters, onFiltersApplied)
    }
}

@Composable
internal fun RemovableFilterChip(label: String, onRemove: () -> Unit) {
    MarketplaceChip(
        label = label,
        selected = false,
        onClick = onRemove,
        style = MarketplaceChipStyle.Active,
        trailingIcon = Icons.Outlined.Close,
        description = "Remove filter: $label"
    )
}

/** The badge counts filters beyond category and sort unless those live in the sheet. */
internal fun MarketplaceFilterState.activeFilterCount(includeCategoryAndSort: Boolean): Int {
    var count = attributes.activeCount
    if (condition != null) count++
    if (!priceRange.isEmpty) count++
    if (includeCategoryAndSort) {
        if (category != null) count++
        if (sortOrder != MarketplaceSortOrder.NEWEST) count++
    }
    return count
}

private fun MarketplacePriceRange.displayName(): String = when {
    minimumCents != null && maximumCents != null ->
        "${formatCents(minimumCents)}–${formatCents(maximumCents)}"
    minimumCents != null -> "${formatCents(minimumCents)}+"
    maximumCents != null -> "Up to ${formatCents(maximumCents)}"
    else -> "Any price"
}

private fun formatCents(cents: Long): String = "\$${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"
