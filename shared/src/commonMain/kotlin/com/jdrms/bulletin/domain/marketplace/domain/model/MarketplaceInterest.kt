package com.jdrms.bulletin.domain.marketplace.domain.model

data class MarketplaceInterest(
    val category: MarketplaceCategory,
    val condition: MarketplaceCondition? = null,
    val priceRange: MarketplacePriceRange = MarketplacePriceRange.NONE,
    val attributes: MarketplaceAttributeFilters = MarketplaceAttributeFilters.NONE
) {
    init {
        require(category.supportsCondition || condition == null) {
            "Housing interests cannot filter by item condition."
        }
        require(attributes.isValidFor(category)) {
            "Marketplace interest filters must apply to their category."
        }
    }

    val activeFilterCount: Int
        get() = attributes.allPredicates.size +
            (if (condition == null) 0 else 1) +
            (if (priceRange.isEmpty) 0 else 1)
}
