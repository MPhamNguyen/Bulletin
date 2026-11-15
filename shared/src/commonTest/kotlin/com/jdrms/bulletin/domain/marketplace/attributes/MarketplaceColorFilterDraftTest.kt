package com.jdrms.bulletin.domain.marketplace.attributes

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributePredicate
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceAttributeFilterDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MarketplaceColorFilterDraftTest {
    @Test
    fun addsRemovesAndRoundTripsMultipleSelections() {
        val draft = MarketplaceAttributeFilterDraft().toggleColor("red").toggleColor("orange").toggleColor("blue")
        val filters = assertNotNull(draft.toFilters(MarketplaceCategory.CLOTHING).filters)
        assertEquals(
            MarketplaceAttributePredicate.ChoiceOptions(setOf("red", "orange", "blue")),
            filters.predicates["color"]
        )
        val restored = MarketplaceAttributeFilterDraft.from(filters)
        assertEquals(filters, restored.toFilters(MarketplaceCategory.CLOTHING).filters)
        val removed = restored.toggleColor("orange")
        assertEquals(setOf("red", "blue"), removed.input("color").choices)
        val cleared = removed.toggleColor("red").toggleColor("blue")
        assertTrue(assertNotNull(cleared.toFilters(MarketplaceCategory.CLOTHING).filters).isEmpty)
        assertTrue(draft.reconciled(MarketplaceCategory.ELECTRONICS).inputs.isEmpty())
    }

    @Test
    fun preservesExistingSingleSelectionWhenAddingAColor() {
        val draft = MarketplaceAttributeFilterDraft().withExact("color", "red").toggleColor("blue")
        assertEquals(setOf("red", "blue"), draft.input("color").choices)
        val invalid = draft.toggleColor("invalid").toFilters(MarketplaceCategory.CLOTHING)
        assertTrue(invalid.errors.containsKey("color"))
    }
}
