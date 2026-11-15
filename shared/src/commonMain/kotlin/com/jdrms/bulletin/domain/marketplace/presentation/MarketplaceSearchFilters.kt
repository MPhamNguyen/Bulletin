package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Badge
import androidx.compose.material3.FilledIconButton
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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.BulletinFieldDecoration
import com.jdrms.bulletin.core.designsystem.BulletinTextField
import com.jdrms.bulletin.core.designsystem.theme.BulletinFieldStyle
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing

/**
 * Search field, filter button, optional [externalControls] (category chips and sort), and applied-filter chips.
 * When [externalControls] is supplied it owns category and sort, so the filter sheet and badge leave them out.
 */
@Composable
internal fun MarketplaceSearchFilters(
    filters: MarketplaceFilterState,
    onQueryChanged: (String) -> Unit,
    onFiltersApplied: (MarketplaceFilterState) -> Unit,
    onSearchPositioned: (LayoutCoordinates) -> Unit,
    externalControls: (@Composable () -> Unit)? = null
) {
    var isFilterSheetOpen by remember { mutableStateOf(false) }
    val sheetOwnsCategoryAndSort = externalControls == null
    Column(verticalArrangement = Arrangement.spacedBy(BulletinSpacing.m)) {
        MarketplaceSearchBar(
            filters = filters,
            activeCount = filters.activeFilterCount(sheetOwnsCategoryAndSort),
            onQueryChanged = onQueryChanged,
            onOpenFilters = { isFilterSheetOpen = true },
            onSearchPositioned = onSearchPositioned
        )
        externalControls?.invoke()
        if (isFilterSheetOpen) {
            MarketplaceFilterSheet(
                filters = filters,
                ownsCategoryAndSort = sheetOwnsCategoryAndSort,
                onApply = {
                    onFiltersApplied(it)
                    isFilterSheetOpen = false
                },
                onDismiss = { isFilterSheetOpen = false }
            )
        }
        ActiveMarketplaceFilterChips(filters, sheetOwnsCategoryAndSort, onFiltersApplied)
    }
}

@Composable
private fun MarketplaceSearchBar(
    filters: MarketplaceFilterState,
    activeCount: Int,
    onQueryChanged: (String) -> Unit,
    onOpenFilters: () -> Unit,
    onSearchPositioned: (LayoutCoordinates) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(BulletinSpacing.m),
        verticalAlignment = Alignment.CenterVertically
    ) {
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
            ),
            style = BulletinFieldStyle.Search
        )
        FilterButton(activeCount, onOpenFilters)
    }
}

@Composable
private fun FilterButton(activeCount: Int, onClick: () -> Unit) {
    val description = if (activeCount == 0) "Show filters" else "Show filters, $activeCount active"
    Box {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.semantics { contentDescription = description },
            shape = MaterialTheme.shapes.small
        ) {
            Icon(Icons.Outlined.Tune, contentDescription = null)
        }
        if (activeCount > 0) {
            Badge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp)
                    .heightIn(min = 20.dp),
                containerColor = MaterialTheme.colorScheme.onBackground,
                contentColor = MaterialTheme.colorScheme.background
            ) {
                Text(activeCount.toString(), style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
