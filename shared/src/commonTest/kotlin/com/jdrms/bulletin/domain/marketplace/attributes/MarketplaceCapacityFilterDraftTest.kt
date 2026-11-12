package com.jdrms.bulletin.domain.marketplace.attributes

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCapacityOptions
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCapacityRange
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceAttributeFilterDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarketplaceCapacityFilterDraftTest {
    @Test
    fun multipleSelectionsApplyRoundTripAndToggleOffIndependently() {
        val first = MarketplaceCapacityOptions.ram[2]
        val top = MarketplaceCapacityOptions.ram.last()
        val storage = MarketplaceCapacityOptions.storage.last()
        val draft = MarketplaceAttributeFilterDraft()
            .toggleCapacity("ram_gb", first).toggleCapacity("ram_gb", top)
            .toggleCapacity("storage_gb", storage)
        val result = draft.toFilters(MarketplaceCategory.ELECTRONICS)
        assertTrue(result.errors.isEmpty())
        assertEquals(setOf(first, top), draft.input("ram_gb").capacities)
        assertEquals(draft, MarketplaceAttributeFilterDraft.from(requireNotNull(result.filters)))
        val removed = draft.toggleCapacity("ram_gb", first).toggleCapacity("ram_gb", top)
            .toFilters(MarketplaceCategory.ELECTRONICS)
        assertEquals(setOf("storage_gb"), removed.filters?.allPredicates?.keys)
    }

    @Test
    fun togglingAnotherCapacityPreservesPreviouslyAppliedExactCapacity() {
        val previous = MarketplaceAttributeFilters(ramGb = MarketplaceCapacityRange(16, 16))
        val draft = MarketplaceAttributeFilterDraft.from(previous)
            .toggleCapacity("ram_gb", MarketplaceCapacityOptions.ram.last())
        assertEquals(
            setOf(MarketplaceCapacityRange(16, 16), MarketplaceCapacityOptions.ram.last()),
            draft.input("ram_gb").capacities
        )
    }

    @Test
    fun categoryReconciliationAndResetClearSelections() {
        val draft = MarketplaceAttributeFilterDraft().toggleCapacity("ram_gb", MarketplaceCapacityOptions.ram.last())
        assertTrue(draft.reconciled(MarketplaceCategory.CLOTHING).inputs.isEmpty())
        assertTrue(MarketplaceAttributeFilterDraft().toFilters(MarketplaceCategory.ELECTRONICS).filters!!.isEmpty)
    }
}
