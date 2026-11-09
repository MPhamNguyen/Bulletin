package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MarketplaceFilterStateTest {
    @Test
    fun selectingHousingClearsAnExistingCondition() {
        val filters = MarketplaceFilterState(
            category = MarketplaceCategory.HOUSING,
            condition = MarketplaceCondition.LIKE_NEW
        )

        assertNull(filters.normalized().condition)
    }

    @Test
    fun conditionRemainsForCategoriesWhereItApplies() {
        val filters = MarketplaceFilterState(
            category = MarketplaceCategory.FURNITURE,
            condition = MarketplaceCondition.LIKE_NEW
        )

        assertEquals(MarketplaceCondition.LIKE_NEW, filters.normalized().condition)
    }
}
