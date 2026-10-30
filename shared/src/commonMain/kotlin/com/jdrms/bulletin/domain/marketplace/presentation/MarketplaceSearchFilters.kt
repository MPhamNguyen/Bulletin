package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
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
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition

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
        OutlinedTextField(
            value = filters.query,
            onValueChange = onQueryChanged,
            placeholder = { Text("Search textbooks, furniture, etc.") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .weight(1f)
                .onGloballyPositioned(onSearchPositioned)
                .semantics { contentDescription = "Search marketplace listings" }
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
            shape = RoundedCornerShape(16.dp)
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
        horizontalArrangement = Arrangement.spacedBy(8.dp)
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
        ActiveAttributeFilterChips(filters, onFiltersApplied)
    }
}

private val MarketplaceFilterState.activeFilterCount: Int
    get() = listOfNotNull(category, condition).size + attributes.activeCount

internal fun MarketplaceCategory.displayName(): String = name.toDisplayName()

internal fun MarketplaceCondition.displayName(): String = name.toDisplayName()

private fun String.toDisplayName(): String = lowercase()
    .replace('_', ' ')
    .replaceFirstChar { it.uppercase() }
