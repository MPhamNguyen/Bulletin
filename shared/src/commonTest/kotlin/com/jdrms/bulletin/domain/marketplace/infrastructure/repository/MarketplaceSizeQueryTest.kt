package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributePredicate
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import io.github.jan.supabase.postgrest.PropertyConversionMethod
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

class MarketplaceSizeQueryTest {
    @Test
    fun queriesEitherSizeAndRequiresClothingTypeAndCategory() {
        val builder = PostgrestFilterBuilder(PropertyConversionMethod.NONE)
        builder.applyAttributeFilters(
            MarketplaceAttributeFilters(
                predicates = mapOf(
                    "size" to MarketplaceAttributePredicate.ChoiceOptions(setOf("S", "M")),
                    "clothing_type" to MarketplaceAttributePredicate.Choice("top")
                )
            ),
            MarketplaceCategory.CLOTHING
        )
        assertEquals(listOf("in.(M,S)"), builder.params["attributes->>size"])
        assertEquals(listOf("eq.top"), builder.params["attributes->>clothing_type"])
        assertEquals(listOf("eq.CLOTHING"), builder.params["category"])
    }
}
