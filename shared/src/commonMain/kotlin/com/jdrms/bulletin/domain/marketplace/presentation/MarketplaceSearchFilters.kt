package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinFieldDecoration
import com.jdrms.bulletin.core.designsystem.BulletinTextField
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePriceRange
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceSortOrder

@Composable
internal fun MarketplaceSearchFilters(
    filters: MarketplaceFilterState,
    onQueryChanged: (String) -> Unit,
    onFiltersApplied: (MarketplaceFilterState) -> Unit,
    onSearchPositioned: (androidx.compose.ui.layout.LayoutCoordinates) -> Unit
) {
    var isFilterSheetOpen by remember { mutableStateOf(false) }
    Column(
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MarketplaceSearchBar(
            filters = filters,
            onQueryChanged = onQueryChanged,
            isFilterSheetOpen = isFilterSheetOpen,
            onToggleFilters = { isFilterSheetOpen = true },
            onSearchPositioned = onSearchPositioned
        )
        if (isFilterSheetOpen) {
            MarketplaceFilterSheet(
                filters = filters,
                onApply = {
                    onFiltersApplied(it)
                    isFilterSheetOpen = false
                },
                onDismiss = { isFilterSheetOpen = false }
            )
        }
        ActiveMarketplaceFilterChips(filters, onFiltersApplied)
    }
}

@Composable
private fun MarketplaceSearchBar(
    filters: MarketplaceFilterState,
    onQueryChanged: (String) -> Unit,
    isFilterSheetOpen: Boolean,
    onToggleFilters: () -> Unit,
    onSearchPositioned: (androidx.compose.ui.layout.LayoutCoordinates) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        BulletinTextField(
            value = filters.query,
            onValueChange = onQueryChanged,
            modifier = Modifier
                .weight(1f)
                .onGloballyPositioned(onSearchPositioned)
                .semantics { contentDescription = "Search marketplace listings" },
            decoration = BulletinFieldDecoration(
                placeholder = { Text("Search textbooks, furniture, etc.") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) }
            )
        )
        FilterButton(filters.activeFilterCount, isFilterSheetOpen, onToggleFilters)
    }
}

@Composable
private fun FilterButton(activeCount: Int, isExpanded: Boolean, onClick: () -> Unit) {
    BadgedBox(
        badge = { if (activeCount > 0) Badge { Text(activeCount.toString()) } }
    ) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.semantics {
                contentDescription = when {
                    isExpanded -> "Hide filters"
                    activeCount == 0 -> "Show filters"
                    else -> "Show filters, $activeCount active"
                }
            },
            shape = MaterialTheme.shapes.large
        ) {
            Icon(Icons.Outlined.Tune, contentDescription = null)
        }
    }
}

@Composable
private fun ActiveMarketplaceFilterChips(
    filters: MarketplaceFilterState,
    onFiltersApplied: (MarketplaceFilterState) -> Unit
) {
    if (filters.activeFilterCount == 0) return
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.s)
    ) {
        filters.category?.let { category ->
            FilterChip(
                selected = true,
                onClick = { onFiltersApplied(filters.copy(category = null)) },
                label = { Text("${category.displayName()} ×") },
            )
        }
        filters.condition?.let { condition ->
            FilterChip(
                selected = true,
                onClick = { onFiltersApplied(filters.copy(condition = null)) },
                label = { Text("${condition.displayName()} ×") },
            )
        }
        if (!filters.priceRange.isEmpty) {
            FilterChip(
                selected = true,
                onClick = { onFiltersApplied(filters.copy(priceRange = MarketplacePriceRange.NONE)) },
                label = { Text("${filters.priceRange.displayName()} ×") },
            )
        }
        if (filters.sortOrder != MarketplaceSortOrder.NEWEST) {
            FilterChip(
                selected = true,
                onClick = { onFiltersApplied(filters.copy(sortOrder = MarketplaceSortOrder.NEWEST)) },
                label = { Text("${filters.sortOrder.displayName()} ×") },
            )
        }
        ActiveAttributeFilterChips(filters, onFiltersApplied)
    }
}

private val MarketplaceFilterState.activeFilterCount: Int
    get() = listOfNotNull(category, condition).size + attributes.activeCount +
        (if (priceRange.isEmpty) 0 else 1) +
        (if (sortOrder == MarketplaceSortOrder.NEWEST) 0 else 1)

private fun MarketplacePriceRange.displayName(): String = when {
    minimumCents != null && maximumCents != null ->
        "${formatCents(minimumCents)}–${formatCents(maximumCents)}"
    minimumCents != null -> "${formatCents(minimumCents)}+"
    maximumCents != null -> "Up to ${formatCents(maximumCents)}"
    else -> "Any price"
}

private fun formatCents(cents: Long): String = "\$${cents / 100}.${(cents % 100).toString().padStart(2, '0')}"

internal fun MarketplaceCategory.displayName(): String = name.toDisplayName()

internal fun MarketplaceCondition.displayName(): String = name.toDisplayName()

private fun String.toDisplayName(): String = lowercase()
    .replace('_', ' ')
    .replaceFirstChar { it.uppercase() }
