package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.unit.dp
import com.jdrms.bulletin.core.designsystem.theme.BulletinSpacing
import com.jdrms.bulletin.domain.marketplace.application.MarketplaceLayoutPreference
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceSortOrder

internal class MarketplaceCatalogHeaderActions(
    val onQueryChanged: (String) -> Unit,
    val onFiltersApplied: (MarketplaceFilterState) -> Unit,
    val onCategorySelected: (MarketplaceCategory?) -> Unit,
    val onLayoutChanged: (MarketplaceLayoutPreference) -> Unit,
    val onSortSelected: (MarketplaceSortOrder) -> Unit,
    val onSearchPositioned: (LayoutCoordinates) -> Unit
)

/** Title, search + filter button, category chips, layout/sort row, and applied-filter chips. */
internal fun LazyGridScope.marketplaceCatalogHeader(
    state: MarketplaceUiState,
    actions: MarketplaceCatalogHeaderActions
) {
    item(span = { GridItemSpan(maxLineSpan) }) {
        Text(
            text = "Marketplace",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(top = BulletinSpacing.s, bottom = 6.dp)
        )
    }
    item(span = { GridItemSpan(maxLineSpan) }) {
        MarketplaceSearchFilters(
            filters = state.filters,
            onQueryChanged = actions.onQueryChanged,
            onFiltersApplied = actions.onFiltersApplied,
            onSearchPositioned = actions.onSearchPositioned,
            externalControls = { MarketplaceCatalogControls(state, actions) }
        )
    }
}

@Composable
private fun MarketplaceCatalogControls(state: MarketplaceUiState, actions: MarketplaceCatalogHeaderActions) {
    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(BulletinSpacing.m)) {
        MarketplaceCategoryChips(state.filters.category, actions.onCategorySelected)
        MarketplaceToolRow(
            layout = state.layoutPreference,
            onLayoutChanged = actions.onLayoutChanged,
            sortOrder = state.filters.sortOrder,
            onSortSelected = actions.onSortSelected
        )
    }
}
