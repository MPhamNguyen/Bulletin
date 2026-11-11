package com.jdrms.bulletin.domain.marketplace.infrastructure.mapper

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCapacityRange
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceInterest
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplacePriceRange
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.MarketplaceInterestDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class MarketplaceInterestMapperTest {
    @Test
    fun roundTripsDetailedInterestFilters() {
        val interest = MarketplaceInterest(
            category = MarketplaceCategory.ELECTRONICS,
            condition = MarketplaceCondition.GOOD,
            priceRange = MarketplacePriceRange(10_000, 50_000),
            attributes = MarketplaceAttributeFilters.create(
                deviceType = "laptop",
                ramGb = MarketplaceCapacityRange(16, 16)
            )
        )

        val restored = MarketplaceInterestMapper.toDomain(MarketplaceInterestMapper.toDto(interest))

        assertEquals(interest.category, restored.category)
        assertEquals(interest.condition, restored.condition)
        assertEquals(interest.priceRange, restored.priceRange)
        assertEquals(interest.attributes.allPredicates, restored.attributes.allPredicates)
    }

    @Test
    fun rejectsUnknownPersistedCategory() {
        val failure = assertFailsWith<IllegalStateException> {
            MarketplaceInterestMapper.toDomain(MarketplaceInterestDto("SERVICES"))
        }

        assertEquals("Unknown marketplace interest category: SERVICES", failure.message)
    }
}
