package com.jdrms.bulletin.domain.listings.attributes

import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeCategoryTransition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ListingAttributeCategoryTransitionTest {
    private val electronics = mapOf(
        "brand" to "Framework",
        "device_type" to "laptop",
        "ram_gb" to "16",
        "storage_gb" to "512"
    )

    @Test
    fun compatibleBrandSurvivesAndIncompatibleValuesAreReported() {
        val plan = ListingAttributeCategoryTransition.plan(
            ListingCategory.ELECTRONICS,
            ListingCategory.CLOTHING,
            electronics
        )
        assertEquals(mapOf("brand" to "Framework"), plan.retained)
        assertEquals(setOf("device_type", "ram_gb", "storage_gb"), plan.discarded.map { it.key }.toSet())
        assertTrue(plan.discardsValues)
    }

    @Test
    fun blankValuesAreNotCountedAsDiscarded() {
        val plan = ListingAttributeCategoryTransition.plan(
            ListingCategory.ELECTRONICS,
            ListingCategory.TEXTBOOKS,
            mapOf("brand" to "", "ram_gb" to "  ")
        )
        assertFalse(plan.discardsValues)
        assertTrue(plan.retained.isEmpty())
    }

    @Test
    fun hiddenDependentInputIsNeitherRetainedNorReported() {
        val hidden = mapOf("device_type" to "monitor", "ram_gb" to "8")
        val plan = ListingAttributeCategoryTransition.plan(ListingCategory.ELECTRONICS, ListingCategory.OTHER, hidden)
        assertEquals(listOf("device_type"), plan.discarded.map { it.key })
    }

    @Test
    fun movingToOtherDiscardsEverythingNonblank() {
        val plan = ListingAttributeCategoryTransition.plan(
            ListingCategory.ELECTRONICS,
            ListingCategory.OTHER,
            electronics
        )
        assertTrue(plan.retained.isEmpty())
        assertEquals(4, plan.discarded.size)
    }

    @Test
    fun emptyDraftChangesCategoryWithoutWarning() {
        val plan = ListingAttributeCategoryTransition.plan(
            ListingCategory.HOUSING,
            ListingCategory.FURNITURE,
            emptyMap()
        )
        assertFalse(plan.discardsValues)
    }
}
