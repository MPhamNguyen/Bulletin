package com.jdrms.bulletin.domain.marketplace.attributes

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributePredicate
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeValue
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributes
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCapacityOptions
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCapacityRange
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MarketplaceCapacityOptionsTest {
    @Test
    fun topBucketsIncludeTheirBoundaryAndLargerValues() {
        val ram = MarketplaceCapacityOptions.ram.last()
        val storage = MarketplaceCapacityOptions.storage.last()
        assertEquals("128 GB+", MarketplaceCapacityOptions.label(ram))
        assertEquals("2 TB+", MarketplaceCapacityOptions.label(storage))
        assertFalse(ram.contains(127))
        assertTrue(ram.contains(128))
        assertTrue(ram.contains(256))
        assertFalse(storage.contains(2047))
        assertTrue(storage.contains(2048))
        assertTrue(storage.contains(4096))
    }

    @Test
    fun selectionsMatchEitherCapacityButNotValuesBetweenThemOrMissingAttributes() {
        val filters = MarketplaceAttributeFilters(
            predicates = mapOf(
                "ram_gb" to MarketplaceAttributePredicate.IntegerOptions(
                    setOf(
                        MarketplaceCapacityRange(16, 16), MarketplaceCapacityRange(64, 64),
                        MarketplaceCapacityOptions.ram.last()
                    )
                ),
                "storage_gb" to MarketplaceAttributePredicate.IntegerOptions(
                    setOf(MarketplaceCapacityOptions.storage.last())
                )
            )
        )
        fun matches(ram: Long, storage: Long) = filters.matches(
            MarketplaceAttributes(
                mapOf(
                    "ram_gb" to MarketplaceAttributeValue.Integer(ram),
                    "storage_gb" to MarketplaceAttributeValue.Integer(storage)
                )
            )
        )
        assertTrue(matches(16, 2048))
        assertTrue(matches(64, 4096))
        assertTrue(matches(256, 4096))
        assertFalse(matches(32, 4096))
        assertFalse(matches(16, 1024))
        assertFalse(filters.matches(MarketplaceAttributes.EMPTY))
        assertTrue(filters.isValidFor(MarketplaceCategory.ELECTRONICS))
        assertTrue(filters.reconciledFor(MarketplaceCategory.CLOTHING).isEmpty)
    }

    @Test
    fun emptySelectionCannotBecomeAnActivePredicate() {
        assertFailsWith<IllegalArgumentException> { MarketplaceAttributePredicate.IntegerOptions(emptySet()) }
    }
}
