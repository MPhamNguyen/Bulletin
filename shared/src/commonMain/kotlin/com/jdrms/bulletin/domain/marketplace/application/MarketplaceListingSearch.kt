package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeCatalog
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeMatching
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeValue

internal fun MarketplaceListingSnapshot.matchesSearch(query: String): Boolean {
    val trimmed = query.trim()
    if (trimmed.isEmpty()) return true
    val keywords = trimmed.lowercase().split(Regex("\\s+")).filter(String::isNotEmpty)
    val titleAndCategory = "${title.lowercase()} ${category.name.lowercase()}"
    if (keywords.all(titleAndCategory::contains)) return true

    val brand = (attributes[MarketplaceAttributeCatalog.BRAND] as? MarketplaceAttributeValue.Text)?.value
        ?: return false
    return MarketplaceAttributeMatching.brandKey(brand) == MarketplaceAttributeMatching.brandKey(trimmed)
}
