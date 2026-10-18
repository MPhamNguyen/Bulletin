package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.jdrms.bulletin.core.designsystem.ListingFilterMenus
import com.jdrms.bulletin.core.designsystem.ListingFilterOption
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition

@Composable
fun MarketplaceFilters(
    filters: MarketplaceFilterState,
    onCategorySelected: (MarketplaceCategory?) -> Unit,
    onConditionSelected: (MarketplaceCondition?) -> Unit,
    modifier: Modifier = Modifier
) {
    ListingFilterMenus(
        selectedCategory = filters.category?.name,
        selectedCondition = filters.condition?.name,
        categories = MarketplaceCategory.entries.map { it.toFilterOption() },
        conditions = MarketplaceCondition.knownValues.map { it.toFilterOption() },
        onCategorySelected = { value ->
            onCategorySelected(value?.let(MarketplaceCategory::valueOf))
        },
        onConditionSelected = { value ->
            onConditionSelected(value?.let(MarketplaceCondition::valueOf))
        },
        modifier = modifier
    )
}

private fun MarketplaceCategory.toFilterOption() = ListingFilterOption(name, name.toDisplayName())

private fun MarketplaceCondition.toFilterOption() = ListingFilterOption(name, name.toDisplayName())

private fun String.toDisplayName(): String = lowercase()
    .replace('_', ' ')
    .replaceFirstChar { it.uppercase() }
