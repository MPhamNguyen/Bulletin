package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributePredicate
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePriceRange
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceSortOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MarketplaceCategorySelectionTest {
    private val priced = MarketplacePriceRange(minimumCents = 1_000L, maximumCents = 5_000L)
    private val brandFilter = MarketplaceAttributeFilters(
        predicates = mapOf("brand" to MarketplaceAttributePredicate.ExactText("nike", usesMatchKey = true))
    )

    @Test
    fun selectingTheCurrentCategoryChangesNothing() {
        val filters = MarketplaceFilterState(category = MarketplaceCategory.FURNITURE, priceRange = priced)

        val change = filters.selectCategory(MarketplaceCategory.FURNITURE)

        assertSame(filters, change.filters)
        assertFalse(change.clearedOtherFilters)
    }

    @Test
    fun switchingCategoryKeepsPriceConditionAndSort() {
        val filters = MarketplaceFilterState(
            category = MarketplaceCategory.FURNITURE,
            condition = MarketplaceCondition.GOOD,
            priceRange = priced,
            sortOrder = MarketplaceSortOrder.PRICE_LOW_TO_HIGH
        )

        val change = filters.selectCategory(MarketplaceCategory.ELECTRONICS)

        assertEquals(MarketplaceCategory.ELECTRONICS, change.filters.category)
        assertEquals(MarketplaceCondition.GOOD, change.filters.condition)
        assertEquals(priced, change.filters.priceRange)
        assertEquals(MarketplaceSortOrder.PRICE_LOW_TO_HIGH, change.filters.sortOrder)
        assertFalse(change.clearedOtherFilters)
    }

    @Test
    fun switchingCategoryDropsCategoryAttributesAndReportsTheReset() {
        val filters = MarketplaceFilterState(category = MarketplaceCategory.CLOTHING, attributes = brandFilter)

        val change = filters.selectCategory(MarketplaceCategory.FURNITURE)

        assertTrue(change.filters.attributes.isEmpty)
        assertTrue(change.clearedOtherFilters)
    }

    @Test
    fun enteringHousingDropsPriceAndCondition() {
        val filters = MarketplaceFilterState(condition = MarketplaceCondition.GOOD, priceRange = priced)

        val change = filters.selectCategory(MarketplaceCategory.HOUSING)

        assertNull(change.filters.condition)
        assertTrue(change.filters.priceRange.isEmpty)
        assertTrue(change.clearedOtherFilters)
    }

    @Test
    fun leavingHousingDropsPriceAndCondition() {
        val filters = MarketplaceFilterState(
            category = MarketplaceCategory.HOUSING,
            condition = MarketplaceCondition.GOOD,
            priceRange = priced
        )

        val change = filters.selectCategory(null)

        assertNull(change.filters.condition)
        assertTrue(change.filters.priceRange.isEmpty)
        assertTrue(change.clearedOtherFilters)
    }

    @Test
    fun switchingCategoryWithNothingElseAppliedReportsNoReset() {
        val change = MarketplaceFilterState().selectCategory(MarketplaceCategory.TEXTBOOKS)

        assertEquals(MarketplaceCategory.TEXTBOOKS, change.filters.category)
        assertFalse(change.clearedOtherFilters)
    }
}
