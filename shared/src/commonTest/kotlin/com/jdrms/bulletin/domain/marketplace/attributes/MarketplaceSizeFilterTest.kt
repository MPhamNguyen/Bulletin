package com.jdrms.bulletin.domain.marketplace.attributes

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributePredicate
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeValue
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributes
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarketplaceSizeFilterTest {
    private val filters = MarketplaceAttributeFilters(
        predicates = mapOf(
            "size" to MarketplaceAttributePredicate.ChoiceOptions(setOf("S", "M")),
            "clothing_type" to MarketplaceAttributePredicate.Choice("top")
        )
    )

    @Test
    fun matchesEitherSizeAndRequiresClothingType() {
        fun matches(size: String, type: String) = filters.matches(
            MarketplaceAttributes(
                mapOf(
                    "size" to MarketplaceAttributeValue.Choice(size),
                    "clothing_type" to MarketplaceAttributeValue.Choice(type)
                )
            )
        )
        assertTrue(matches("S", "top"))
        assertTrue(matches("M", "top"))
        assertFalse(matches("L", "top"))
        assertFalse(matches("S", "bottom"))
        assertFalse(filters.matches(MarketplaceAttributes.EMPTY))
        assertTrue(filters.isValidFor(MarketplaceCategory.CLOTHING))
        assertTrue(filters.reconciledFor(MarketplaceCategory.ELECTRONICS).isEmpty)
        val invalid = filters.copy(
            predicates = filters.predicates +
                ("size" to MarketplaceAttributePredicate.ChoiceOptions(setOf("S", "32")))
        )
        assertFalse(invalid.isValidFor(MarketplaceCategory.CLOTHING))
    }
}
