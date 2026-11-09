package com.jdrms.bulletin.domain.marketplace.domain.service

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeCatalog
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeMatching
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeValue
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePriceRange
import com.jdrms.bulletin.domain.marketplace.domain.model.supportsCondition

class MarketplaceSearchPolicy {
    fun filterItems(
        items: List<MarketplaceItem>,
        query: String,
        category: MarketplaceCategory?,
        condition: MarketplaceCondition? = null,
        attributes: MarketplaceAttributeFilters = MarketplaceAttributeFilters.NONE,
        priceRange: MarketplacePriceRange = MarketplacePriceRange.NONE
    ): List<MarketplaceItem> {
        val keywords = query.trim().lowercase().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return items.filter { item ->
            val matchesCategory = category == null || item.category == category
            val matchesCondition = condition == null || item.category.supportsCondition && item.condition == condition
            val categoryName = item.category.name.replace('_', ' ').lowercase()
            val searchableText = "${item.title.lowercase()} $categoryName"
            val brand = (item.attributes[MarketplaceAttributeCatalog.BRAND] as? MarketplaceAttributeValue.Text)?.value
            val matchesQuery = keywords.all { keyword -> searchableText.contains(keyword) } ||
                brand != null && MarketplaceAttributeMatching.brandKey(brand) ==
                MarketplaceAttributeMatching.brandKey(query)
            matchesCategory && matchesCondition && priceRange.contains(item.price.amount) &&
                attributes.matches(item.attributes) && matchesQuery
        }
    }
}
