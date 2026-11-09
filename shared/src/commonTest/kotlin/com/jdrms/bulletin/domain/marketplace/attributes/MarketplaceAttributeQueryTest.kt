package com.jdrms.bulletin.domain.marketplace.attributes

import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributePredicate
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCapacityRange
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.applyMarketplaceSearch
import io.github.jan.supabase.postgrest.PropertyConversionMethod
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlin.test.Test
import kotlin.test.assertEquals

class MarketplaceAttributeQueryTest {
    @Test
    fun filtersBecomeAllowlistedServerPredicatesWithExactCategory() {
        val builder = PostgrestFilterBuilder(PropertyConversionMethod.NONE)
        builder.applyMarketplaceSearch(
            MarketplacePageRequest(
                category = MarketplaceCategory.ELECTRONICS,
                attributeFilters = MarketplaceAttributeFilters.create(
                    deviceType = "laptop",
                    brandText = " H&M ",
                    ramGb = MarketplaceCapacityRange(8, 32),
                    storageGb = MarketplaceCapacityRange(min = 256)
                )
            )
        )
        assertEquals(listOf("ilike.ELECTRONICS", "eq.ELECTRONICS"), builder.params["category"])
        assertEquals(listOf("eq.laptop"), builder.params["attr_device_type"])
        assertEquals(listOf("eq.h and m"), builder.params["attr_brand_key"])
        assertEquals(listOf("gte.8", "lte.32"), builder.params["attr_ram_gb"])
        assertEquals(listOf("gte.256"), builder.params["attr_storage_gb"])
    }

    @Test
    fun nonElectronicsFiltersUseFixedJsonAttributePaths() {
        val builder = PostgrestFilterBuilder(PropertyConversionMethod.NONE)
        builder.applyMarketplaceSearch(
            MarketplacePageRequest(
                category = MarketplaceCategory.FURNITURE,
                attributeFilters = MarketplaceAttributeFilters(
                    predicates = mapOf(
                        "material" to MarketplaceAttributePredicate.Choice("wood"),
                        "width_cm" to MarketplaceAttributePredicate.DecimalRange(max = 90.5),
                        "length_cm" to MarketplaceAttributePredicate.DecimalRange(min = 120.0)
                    )
                )
            )
        )

        assertEquals(listOf("ilike.FURNITURE", "eq.FURNITURE"), builder.params["category"])
        assertEquals(listOf("eq.wood"), builder.params["attributes->>material"])
        assertEquals(listOf("lte.90.5"), builder.params["attributes->width_cm"])
        assertEquals(listOf("gte.120.0"), builder.params["attributes->length_cm"])
    }

    @Test
    fun exactTextFiltersDoNotTreatWildcardsAsPatterns() {
        val builder = PostgrestFilterBuilder(PropertyConversionMethod.NONE)
        builder.applyMarketplaceSearch(
            MarketplacePageRequest(
                category = MarketplaceCategory.TEXTBOOKS,
                attributeFilters = MarketplaceAttributeFilters(
                    predicates = mapOf("course_code" to MarketplaceAttributePredicate.ExactText("CECS 328"))
                )
            )
        )

        assertEquals(listOf("eq.CECS 328"), builder.params["attributes->>course_code"])
    }
}
