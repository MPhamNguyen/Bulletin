package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePriceRange
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceSortOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MarketplaceFilterSheetStateTest {
    @Test
    fun resetIsDisabledUntilSomethingIsSelected() {
        val state = MarketplaceFilterSheetState(MarketplaceFilterState(), ownsCategoryAndSort = false)
        assertFalse(state.hasActiveFilters)

        state.selectCondition(MarketplaceCondition.GOOD)

        assertTrue(state.hasActiveFilters)
    }

    @Test
    fun resetKeepsQueryCategoryAndSortWhenTheMainScreenOwnsThem() {
        val initial = MarketplaceFilterState(
            query = "desk",
            category = MarketplaceCategory.FURNITURE,
            condition = MarketplaceCondition.GOOD,
            sortOrder = MarketplaceSortOrder.PRICE_HIGH_TO_LOW
        )
        val state = MarketplaceFilterSheetState(initial, ownsCategoryAndSort = false)
        state.updatePrice(MarketplacePriceFilterDraft(minimumCents = "1000"))

        state.reset()

        assertEquals(MarketplaceCategory.FURNITURE, state.draft.category)
        assertEquals(MarketplaceSortOrder.PRICE_HIGH_TO_LOW, state.draft.sortOrder)
        assertEquals("desk", state.draft.query)
        assertNull(state.draft.condition)
        assertFalse(state.hasActiveFilters)
    }

    @Test
    fun resetAlsoClearsCategoryAndSortWhenTheSheetOwnsThem() {
        val initial = MarketplaceFilterState(
            category = MarketplaceCategory.FURNITURE,
            sortOrder = MarketplaceSortOrder.PRICE_HIGH_TO_LOW
        )
        val state = MarketplaceFilterSheetState(initial, ownsCategoryAndSort = true)
        assertTrue(state.hasActiveFilters)

        state.reset()

        assertNull(state.draft.category)
        assertEquals(MarketplaceSortOrder.NEWEST, state.draft.sortOrder)
    }

    @Test
    fun applyRejectsAMinimumAboveTheMaximumAndKeepsTheSheetOpen() {
        val state = MarketplaceFilterSheetState(MarketplaceFilterState(), ownsCategoryAndSort = false)
        state.updatePrice(MarketplacePriceFilterDraft(minimumCents = "5000", maximumCents = "1000"))

        assertNull(state.apply())
        assertNotNull(state.priceError)
    }

    @Test
    fun applyReturnsThePriceRangeAndCondition() {
        val state = MarketplaceFilterSheetState(MarketplaceFilterState(), ownsCategoryAndSort = false)
        state.selectCondition(MarketplaceCondition.LIKE_NEW)
        state.updatePrice(MarketplacePriceFilterDraft(minimumCents = "1000", maximumCents = "5000"))

        val applied = state.apply()

        assertEquals(MarketplaceCondition.LIKE_NEW, applied?.condition)
        assertEquals(MarketplacePriceRange(1_000L, 5_000L), applied?.priceRange)
    }

    @Test
    fun editingThePriceClearsItsError() {
        val state = MarketplaceFilterSheetState(MarketplaceFilterState(), ownsCategoryAndSort = false)
        state.updatePrice(MarketplacePriceFilterDraft(minimumCents = "5000", maximumCents = "1000"))
        state.apply()

        state.updatePrice(MarketplacePriceFilterDraft(minimumCents = "500", maximumCents = "1000"))

        assertNull(state.priceError)
    }

    @Test
    fun switchingCategoryInTheSheetClearsAttributeInputs() {
        val state = MarketplaceFilterSheetState(
            MarketplaceFilterState(category = MarketplaceCategory.CLOTHING),
            ownsCategoryAndSort = true
        )
        state.updateAttributes(state.attributeDraft.withExact("brand", "nike"))
        assertTrue(state.hasActiveFilters)

        state.selectCategory(MarketplaceCategory.FURNITURE)

        assertTrue(state.attributeDraft.inputs.isEmpty())
    }
}
