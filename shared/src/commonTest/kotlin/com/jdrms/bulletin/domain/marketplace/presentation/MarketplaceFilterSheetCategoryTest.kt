package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePriceRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MarketplaceFilterSheetCategoryTest {
    @Test
    fun categoryChangesApplyTheSameFiltersAsChips() {
        val categories = listOf(null) + MarketplaceCategory.entries
        for (source in categories) {
            for (target in categories) {
                val initial = MarketplaceFilterState(
                    category = source,
                    condition = MarketplaceCondition.GOOD,
                    priceRange = MarketplacePriceRange(1_000L, 5_000L)
                ).normalized()
                val state = MarketplaceFilterSheetState(initial, ownsCategoryAndSort = true)

                state.selectCategory(target)

                assertEquals(initial.selectCategory(target).filters, state.apply(), "$source to $target")
            }
        }
    }

    @Test
    fun crossingHousingClearsUnappliedPriceInputsAndErrors() {
        val transitions = listOf(
            MarketplaceCategory.FURNITURE to MarketplaceCategory.HOUSING,
            MarketplaceCategory.HOUSING to MarketplaceCategory.FURNITURE,
            MarketplaceCategory.HOUSING to null
        )
        for ((source, target) in transitions) {
            val state = MarketplaceFilterSheetState(
                MarketplaceFilterState(category = source),
                ownsCategoryAndSort = true
            )
            state.updatePrice(MarketplacePriceFilterDraft(minimumCents = "5000", maximumCents = "1000"))
            assertNull(state.apply())
            assertNotNull(state.priceError)

            state.selectCategory(target)

            assertEquals(MarketplacePriceFilterDraft(), state.priceDraft)
            assertNull(state.priceError)
            assertEquals(MarketplacePriceRange.NONE, assertNotNull(state.apply()).priceRange)
        }
    }

    @Test
    fun ordinaryCategoryChangesAndReselectionKeepUnappliedPriceInputs() {
        for (target in listOf(MarketplaceCategory.FURNITURE, MarketplaceCategory.ELECTRONICS)) {
            val state = MarketplaceFilterSheetState(
                MarketplaceFilterState(category = MarketplaceCategory.FURNITURE),
                ownsCategoryAndSort = true
            )
            state.updatePrice(MarketplacePriceFilterDraft(minimumCents = "1000"))

            state.selectCategory(target)

            assertEquals(MarketplacePriceRange(minimumCents = 1_000L), assertNotNull(state.apply()).priceRange)
        }
    }
}
