package com.jdrms.bulletin.domain.marketplace.infrastructure.mapper

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributePredicate
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCapacityRange
import kotlin.test.Test
import kotlin.test.assertEquals

class MarketplaceInterestFilterJsonMapperTest {
    @Test
    fun roundTripsMultipleChoicesAndCapacityRanges() {
        val filters = MarketplaceAttributeFilters(
            predicates = mapOf(
                "color" to MarketplaceAttributePredicate.ChoiceOptions(setOf("red", "blue")),
                "storage_gb" to MarketplaceAttributePredicate.IntegerOptions(
                    setOf(MarketplaceCapacityRange(128, 128), MarketplaceCapacityRange(256, 512))
                )
            )
        )

        val restored = MarketplaceInterestFilterJsonMapper.decode(MarketplaceInterestFilterJsonMapper.encode(filters))

        assertEquals(filters.allPredicates, restored.allPredicates)
    }
}
