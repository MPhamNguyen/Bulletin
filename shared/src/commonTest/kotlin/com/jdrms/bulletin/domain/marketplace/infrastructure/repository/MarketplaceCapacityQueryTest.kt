package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributePredicate
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCapacityOptions
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCapacityRange
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import io.github.jan.supabase.postgrest.PropertyConversionMethod
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MarketplaceCapacityQueryTest {
    @Test
    fun capacitiesUseSeparateDisjunctionsWithinEachAttribute() {
        val builder = PostgrestFilterBuilder(PropertyConversionMethod.NONE)
        builder.applyAttributeFilters(
            MarketplaceAttributeFilters(
                predicates = mapOf(
                    "ram_gb" to MarketplaceAttributePredicate.IntegerOptions(
                        setOf(
                            MarketplaceCapacityRange(16, 16), MarketplaceCapacityOptions.ram.last()
                        )
                    ),
                    "storage_gb" to MarketplaceAttributePredicate.IntegerOptions(
                        setOf(
                            MarketplaceCapacityRange(512, 512), MarketplaceCapacityOptions.storage.last()
                        )
                    )
                )
            ),
            MarketplaceCategory.ELECTRONICS
        )
        val query = builder.params["and"].orEmpty().joinToString()
        assertEquals(2, Regex("or\\(").findAll(query).count())
        assertTrue("attr_ram_gb.eq.16" in query)
        assertTrue("attr_ram_gb.gte.128" in query)
        assertTrue("attr_storage_gb.eq.512" in query)
        assertTrue("attr_storage_gb.gte.2048" in query)
    }
}
