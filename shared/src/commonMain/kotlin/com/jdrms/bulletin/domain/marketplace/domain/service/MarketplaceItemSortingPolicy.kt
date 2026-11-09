package com.jdrms.bulletin.domain.marketplace.domain.service

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceSortOrder

class MarketplaceItemSortingPolicy {
    fun sort(items: List<MarketplaceItem>, order: MarketplaceSortOrder): List<MarketplaceItem> =
        items.sortedWith(
            when (order) {
                MarketplaceSortOrder.NEWEST ->
                    compareByDescending<MarketplaceItem> { it.createdAtMillis }.thenByDescending { it.id.value }
                MarketplaceSortOrder.PRICE_LOW_TO_HIGH ->
                    compareBy<MarketplaceItem> { it.price.amount }.thenBy { it.id.value }
                MarketplaceSortOrder.PRICE_HIGH_TO_LOW ->
                    compareByDescending<MarketplaceItem> { it.price.amount }.thenByDescending { it.id.value }
            }
        )
}
