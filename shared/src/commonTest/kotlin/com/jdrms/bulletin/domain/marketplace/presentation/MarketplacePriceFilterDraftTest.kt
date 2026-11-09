package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePriceRange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class MarketplacePriceFilterDraftTest {
    @Test
    fun blankBoundsRemainUnset() {
        val result = MarketplacePriceFilterDraft().toPriceRange()

        assertEquals(MarketplacePriceRange.NONE, result.priceRange)
        assertNull(result.error)
    }

    @Test
    fun reportsAnInvertedRangeWithoutConstructingInvalidState() {
        val result = MarketplacePriceFilterDraft(
            minimumCents = "2001",
            maximumCents = "2000"
        ).toPriceRange()

        assertEquals(MarketplacePriceRange.NONE, result.priceRange)
        assertEquals("Maximum price must be at least the minimum price.", result.error)
    }
}
