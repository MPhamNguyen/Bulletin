package com.jdrms.bulletin.domain.marketplace.attributes

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributePredicate
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.presentation.MarketplaceAttributeFilterDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class MarketplaceSizeFilterDraftTest {
    @Test
    fun selectsMultipleSizesReopensAndTogglesEachOff() {
        val draft = MarketplaceAttributeFilterDraft().withExact("clothing_type", "top")
            .toggleChoice("size", "S").toggleChoice("size", "M")
        val filters = assertNotNull(draft.toFilters(MarketplaceCategory.CLOTHING).filters)
        assertEquals(MarketplaceAttributePredicate.ChoiceOptions(setOf("S", "M")), filters.predicates["size"])
        val restored = MarketplaceAttributeFilterDraft.from(filters)
        assertEquals(draft, restored)
        assertEquals(filters, restored.toFilters(MarketplaceCategory.CLOTHING).filters)
        val removed = restored.toggleChoice("size", "S")
        assertEquals(setOf("M"), removed.input("size").choices)
        val cleared = removed.toggleChoice("size", "M").toFilters(MarketplaceCategory.CLOTHING)
        assertEquals(setOf("clothing_type"), cleared.filters?.predicates?.keys)
    }

    @Test
    fun preservesLegacySingleSizeAndRejectsInvalidChoices() {
        val draft = MarketplaceAttributeFilterDraft().withExact("clothing_type", "top")
            .withExact("size", "S").toggleChoice("size", "M")
        assertEquals(setOf("S", "M"), draft.input("size").choices)
        assertEquals(
            setOf("size"),
            draft.toggleChoice("size", "invalid")
                .toFilters(MarketplaceCategory.CLOTHING).errors.keys
        )
        assertTrue(draft.reconciled(MarketplaceCategory.ELECTRONICS).inputs.isEmpty())
    }

    @Test
    fun changingTypeOrShoeSystemClearsIncompatibleSizes() {
        val draft = MarketplaceAttributeFilterDraft().withExact("clothing_type", "shoes")
            .withExact("shoe_size_system", "us_women")
            .toggleChoice("size", "8").toggleChoice("size", "8.5")
        assertTrue(draft.toFilters(MarketplaceCategory.CLOTHING).errors.isEmpty())
        val eu = draft.withExact("shoe_size_system", "eu").reconciled(MarketplaceCategory.CLOTHING)
        assertTrue(eu.input("size").choices.isEmpty())
        val top = draft.withExact("clothing_type", "top").reconciled(MarketplaceCategory.CLOTHING)
        assertTrue(top.input("size").choices.isEmpty())
    }
}
