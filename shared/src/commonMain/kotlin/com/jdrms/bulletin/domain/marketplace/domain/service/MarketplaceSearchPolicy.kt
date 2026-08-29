package com.jdrms.bulletin.domain.marketplace.domain.service

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem

class MarketplaceSearchPolicy {
    fun filterItems(
        items: List<MarketplaceItem>,
        query: String,
        category: MarketplaceCategory?
    ): List<MarketplaceItem> {
        val trimmedQuery = query.trim().lowercase()
        return items.filter { item ->
            val matchesCategory = category == null || item.category == category
            val matchesQuery = trimmedQuery.isEmpty() ||
                item.title.lowercase().contains(trimmedQuery) ||
                item.description.lowercase().contains(trimmedQuery) ||
                item.sellerName.lowercase().contains(trimmedQuery)
            matchesCategory && matchesQuery
        }
    }
}
