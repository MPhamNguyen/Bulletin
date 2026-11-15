package com.jdrms.bulletin.domain.marketplace.attributes

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributePredicate
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
    fun parsesExactCapacitiesAndCanonicalBrandAndRoundTripsAppliedFilters() {
        val parsed = MarketplaceAttributeFilterDraft()
            .withExact("brand", "Levi's")
            .withExact("ram_gb", "16")
            .withExact("storage_gb", "512")
            .toFilters(MarketplaceCategory.ELECTRONICS)
        val expected = MarketplaceAttributeFilters(
            predicates = mapOf(
                "brand" to MarketplaceAttributePredicate.ExactText("levis", usesMatchKey = true),
                "ram_gb" to MarketplaceAttributePredicate.IntegerRange(MarketplaceCapacityRange(16, 16)),
                "storage_gb" to MarketplaceAttributePredicate.IntegerRange(MarketplaceCapacityRange(512, 512))
            )
        )
        assertEquals(expected, parsed.filters)
        assertEquals(
            expected,
            MarketplaceAttributeFilterDraft.from(expected).toFilters(MarketplaceCategory.ELECTRONICS).filters
        )
    }

    @Test
    fun reportsIndependentErrorsAndDoesNotApplyPartialFilters() {
        val parsed = MarketplaceAttributeFilterDraft()
            .withExact("brand", "---")
            .withExact("ram_gb", "not-a-number")
            .withExact("storage_gb", "x")
            .toFilters(MarketplaceCategory.ELECTRONICS)
        assertNull(parsed.filters)
        assertEquals(setOf("brand", "ram_gb", "storage_gb"), parsed.errors.keys)
    }

    @Test
    fun parsesFiltersForEverySellerAttributeKind() {
        val textbooks = MarketplaceAttributeFilterDraft()
            .withExact("course_code", "cecs-328")
            .withExact("edition", "2")
            .toFilters(MarketplaceCategory.TEXTBOOKS)
        val furniture = MarketplaceAttributeFilterDraft()
            .withExact("material", "wood")
            .withMax("width_cm", "90.5")
            .withMin("length_cm", "120")
            .toFilters(MarketplaceCategory.FURNITURE)
        val housing = MarketplaceAttributeFilterDraft()
            .withMin("available_date", "2026-08-01")
            .withMax("monthly_rent_usd", "1500")
            .toFilters(MarketplaceCategory.HOUSING)

        assertTrue(textbooks.errors.isEmpty())
        assertEquals(
            MarketplaceAttributePredicate.ExactText("CECS 328"),
            textbooks.filters?.predicates?.get("course_code")
        )
        assertEquals(
            MarketplaceAttributePredicate.IntegerRange(MarketplaceCapacityRange(2, 2)),
            textbooks.filters?.predicates?.get("edition")
        )
        assertTrue(furniture.errors.isEmpty())
        assertEquals(
            MarketplaceAttributePredicate.DecimalRange(min = 120.0),
            furniture.filters?.predicates?.get("length_cm")
        )
        assertTrue(housing.errors.isEmpty())
    }

    @Test
    fun rejectsImpossibleAvailabilityDates() {
        val parsed = MarketplaceAttributeFilterDraft()
            .withMin("available_date", "2026-02-30")
            .toFilters(MarketplaceCategory.HOUSING)

        assertNull(parsed.filters)
        assertEquals(setOf("available_date"), parsed.errors.keys)
    }

    @Test
    fun clothingSizeFiltersUseTheSelectedTypeAndShoeSystem() {
        val top = MarketplaceAttributeFilterDraft()
            .withExact("clothing_type", "top")
            .withExact("size", "M")
            .toFilters(MarketplaceCategory.CLOTHING)
        val random = MarketplaceAttributeFilterDraft()
            .withExact("clothing_type", "top")
            .withExact("size", "BANANA")
            .toFilters(MarketplaceCategory.CLOTHING)
        val shoes = MarketplaceAttributeFilterDraft()
            .withExact("clothing_type", "shoes")
            .withExact("shoe_size_system", "us_women")
            .withExact("size", "8.5")
            .toFilters(MarketplaceCategory.CLOTHING)

        assertEquals(MarketplaceAttributePredicate.Choice("M"), top.filters?.predicates?.get("size"))
        assertEquals(setOf("size"), random.errors.keys)
        assertTrue(shoes.errors.isEmpty())
    }

    @Test
    fun changingClothingTypeReconcilesDependentFilterValues() {
        val bottom = MarketplaceAttributeFilterDraft()
            .withExact("clothing_type", "bottom")
            .withExact("size", "32")
        val top = bottom.withExact("clothing_type", "top").reconciled(MarketplaceCategory.CLOTHING)

        assertEquals("top", top.input("clothing_type").exact)
        assertTrue(top.input("size").exact.isEmpty())
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
