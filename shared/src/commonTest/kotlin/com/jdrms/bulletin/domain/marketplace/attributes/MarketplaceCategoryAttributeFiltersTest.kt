package com.jdrms.bulletin.domain.marketplace.attributes

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributePredicate
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeValue
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributes
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCapacityRange
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarketplaceCategoryAttributeFiltersTest {
    @Test
    fun textbookChoiceRangeAndExactTextMatchSellerValues() {
        val attributes = MarketplaceAttributes(
            mapOf(
                "course_code" to MarketplaceAttributeValue.Text("CECS 328"),
                "edition" to MarketplaceAttributeValue.Integer(3)
            )
        )
        val filters = MarketplaceAttributeFilters(
            predicates = mapOf(
                "course_code" to MarketplaceAttributePredicate.ExactText("cecs 328"),
                "edition" to MarketplaceAttributePredicate.IntegerRange(MarketplaceCapacityRange(min = 2))
            )
        )

        assertTrue(filters.isValidFor(MarketplaceCategory.TEXTBOOKS))
        assertTrue(filters.matches(attributes))
        assertFalse(filters.isValidFor(MarketplaceCategory.FURNITURE))
    }

    @Test
    fun furnitureAndHousingRangesMatchTypedSellerValues() {
        val furniture = MarketplaceAttributeFilters(
            predicates = mapOf(
                "material" to MarketplaceAttributePredicate.Choice("wood"),
                "width_cm" to MarketplaceAttributePredicate.DecimalRange(max = 100.0),
                "length_cm" to MarketplaceAttributePredicate.DecimalRange(min = 120.0)
            )
        )
        val housing = MarketplaceAttributeFilters(
            predicates = mapOf(
                "available_date" to MarketplaceAttributePredicate.DateRange(maxIso = "2026-09-01"),
                "monthly_rent_usd" to MarketplaceAttributePredicate.DecimalRange(max = 1_500.0)
            )
        )

        assertTrue(
            furniture.matches(
                MarketplaceAttributes(
                    mapOf(
                        "material" to MarketplaceAttributeValue.Choice("wood"),
                        "width_cm" to MarketplaceAttributeValue.Decimal("90.5"),
                        "length_cm" to MarketplaceAttributeValue.Decimal("125.5")
                    )
                )
            )
        )
        assertTrue(
            housing.matches(
                MarketplaceAttributes(
                    mapOf(
                        "available_date" to MarketplaceAttributeValue.Date("2026-08-15"),
                        "monthly_rent_usd" to MarketplaceAttributeValue.Decimal("1250.00")
                    )
                )
            )
        )
    }
}
