package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceSortOrder

internal fun MarketplaceCategory.displayName(): String = name.toDisplayName()

internal fun MarketplaceCondition.displayName(): String = name.toDisplayName()

internal fun MarketplaceSortOrder.displayName(): String = when (this) {
    MarketplaceSortOrder.NEWEST -> "Newest"
    MarketplaceSortOrder.PRICE_LOW_TO_HIGH -> "Price: low to high"
    MarketplaceSortOrder.PRICE_HIGH_TO_LOW -> "Price: high to low"
}

private fun String.toDisplayName(): String = lowercase()
    .replace('_', ' ')
    .replaceFirstChar { it.uppercase() }
