package com.jdrms.bulletin.domain.marketplace.attributes

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCapacityRange
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceAttributeFilterDraft
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceFilterState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MarketplaceAttributeFilterDraftTest {
    @Test
    fun parsesOpenRangesAndCanonicalBrandAndRoundTripsAppliedFilters() {
        val parsed = MarketplaceAttributeFilterDraft(brand = "Levi's", ramMin = "16", storageMax = "512").toFilters()
        val expected = MarketplaceAttributeFilters.create(
            brandText = "levis",
            ramGb = MarketplaceCapacityRange(min = 16),
            storageGb = MarketplaceCapacityRange(max = 512)
        )
        assertEquals(expected, parsed.filters)
        assertEquals(expected, MarketplaceAttributeFilterDraft.from(expected).toFilters().filters)
    }

    @Test
    fun reportsIndependentErrorsAndDoesNotApplyPartialFilters() {
        val parsed = MarketplaceAttributeFilterDraft(brand = "---", ramMin = "16", ramMax = "8", storageMin = "x")
            .toFilters()
        assertNull(parsed.filters)
        assertEquals(setOf("brand", "ram", "storage"), parsed.errors.keys)
    }

    @Test
    fun leavingElectronicsClearsAttributeFilters() {
        val attributes = MarketplaceAttributeFilters.create(deviceType = "laptop")
        val electronics = MarketplaceFilterState(category = MarketplaceCategory.ELECTRONICS, attributes = attributes)
        assertEquals(electronics, electronics.normalized())
        assertTrue(electronics.copy(category = null).normalized().attributes.isEmpty)
        assertTrue(electronics.copy(category = MarketplaceCategory.CLOTHING).normalized().attributes.isEmpty)
    }
}
