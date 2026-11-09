package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import io.github.jan.supabase.postgrest.PropertyConversionMethod
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder
import kotlin.test.Test
import kotlin.test.assertTrue

class MarketplaceBrandSearchQueryTest {
    @Test
    fun clothingSearchMatchesTitleOrNormalizedBrand() {
        val builder = PostgrestFilterBuilder(PropertyConversionMethod.NONE)

        builder.applyMarketplaceSearch(
            MarketplacePageRequest(query = "Levi's", category = MarketplaceCategory.CLOTHING)
        )

        val disjunction = builder.params["or"].orEmpty().joinToString()
        assertTrue("name.fts.levi:*" in disjunction)
        assertTrue("attr_brand_key.eq.levis" in disjunction)
    }
}
