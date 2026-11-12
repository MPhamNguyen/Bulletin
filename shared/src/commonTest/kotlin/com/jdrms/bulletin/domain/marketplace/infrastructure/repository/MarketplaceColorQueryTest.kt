package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributePredicate
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import io.github.jan.supabase.postgrest.PropertyConversionMethod
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

class MarketplaceColorQueryTest {
    @Test
    fun queriesAnySelectedColorAndRequiresSizeAndCategory() {
        val builder = PostgrestFilterBuilder(PropertyConversionMethod.NONE)
        builder.applyAttributeFilters(
            MarketplaceAttributeFilters(
                predicates = mapOf(
                    "color" to MarketplaceAttributePredicate.ChoiceOptions(setOf("red", "orange", "blue")),
                    "size" to MarketplaceAttributePredicate.Choice("M")
                )
            ),
            MarketplaceCategory.CLOTHING
        )
        assertEquals(listOf("in.(blue,orange,red)"), builder.params["attributes->>color"])
        assertEquals(listOf("eq.M"), builder.params["attributes->>size"])
        assertEquals(listOf("eq.CLOTHING"), builder.params["category"])
    }
}
