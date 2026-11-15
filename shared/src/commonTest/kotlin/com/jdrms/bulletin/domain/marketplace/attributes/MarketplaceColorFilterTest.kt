package com.jdrms.bulletin.domain.marketplace.attributes

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributePredicate
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeValue
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributes
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarketplaceColorFilterTest {
    private val filters = MarketplaceAttributeFilters(
        predicates = mapOf(
            "color" to MarketplaceAttributePredicate.ChoiceOptions(setOf("red", "orange", "blue")),
            "size" to MarketplaceAttributePredicate.Choice("M")
        )
    )

    @Test
    fun matchesAnySelectedColorWhileRequiringOtherAttributes() {
        fun matches(color: String, size: String) = filters.matches(
            MarketplaceAttributes(
                mapOf(
                    "color" to MarketplaceAttributeValue.Choice(color),
                    "size" to MarketplaceAttributeValue.Choice(size)
                )
            )
        )
        listOf("red", "orange", "blue").forEach { assertTrue(matches(it, "M")) }
        assertFalse(matches("green", "M"))
        assertFalse(matches("red", "L"))
        assertFalse(filters.matches(MarketplaceAttributes.EMPTY))
    }

    @Test
    fun validatesColorCodesAndReconcilesCategoryChanges() {
        val colors = filters.without("size")
        assertTrue(colors.isValidFor(MarketplaceCategory.CLOTHING))
        assertTrue(colors.reconciledFor(MarketplaceCategory.ELECTRONICS).isEmpty)
        val invalid = MarketplaceAttributeFilters(
            predicates = mapOf(
                "color" to MarketplaceAttributePredicate.ChoiceOptions(setOf("red", "invalid"))
            )
        )
        assertFalse(invalid.isValidFor(MarketplaceCategory.CLOTHING))
        assertFailsWith<IllegalArgumentException> { MarketplaceAttributePredicate.ChoiceOptions(emptySet()) }
        assertFailsWith<IllegalArgumentException> { MarketplaceAttributePredicate.ChoiceOptions(setOf("")) }
    }
}
