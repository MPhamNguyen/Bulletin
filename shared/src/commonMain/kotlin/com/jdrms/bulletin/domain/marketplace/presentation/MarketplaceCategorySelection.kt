package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePriceRange

/** Result of tapping a category chip; [clearedOtherFilters] drives the "Filters reset" notice. */
internal data class MarketplaceCategoryChange(
    val filters: MarketplaceFilterState,
    val clearedOtherFilters: Boolean
)

/**
 * Switching category drops category-specific attributes. Moving into or out of Housing also drops
 * the price range and condition, because Housing is priced and described differently.
 */
internal fun MarketplaceFilterState.selectCategory(next: MarketplaceCategory?): MarketplaceCategoryChange {
    if (next == category) return MarketplaceCategoryChange(this, clearedOtherFilters = false)
    val crossesHousing = next == MarketplaceCategory.HOUSING || category == MarketplaceCategory.HOUSING
    val updated = copy(
        category = next,
        attributes = MarketplaceAttributeFilters.NONE,
        priceRange = if (crossesHousing) MarketplacePriceRange.NONE else priceRange
    ).normalized()
    val cleared = updated.copy(category = null) != copy(category = null)
    return MarketplaceCategoryChange(updated, clearedOtherFilters = cleared)
}
