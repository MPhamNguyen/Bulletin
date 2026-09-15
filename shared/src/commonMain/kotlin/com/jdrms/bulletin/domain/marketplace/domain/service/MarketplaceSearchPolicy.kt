package com.jdrms.bulletin.domain.marketplace.domain.service

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem

class MarketplaceSearchPolicy {
    fun filterItems(
        items: List<MarketplaceItem>,
        query: String,
        category: MarketplaceCategory?
    ): List<MarketplaceItem> {
        val keywords = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return items.filter { item ->
            val matchesCategory = category == null || item.category == category
            val categoryName = item.category.name.replace('_', ' ').lowercase()
            val searchableText = "${item.title.lowercase()} $categoryName"
            val matchesQuery = keywords.all { keyword -> searchableText.contains(keyword) }
            matchesCategory && matchesQuery
        }
    }
}
