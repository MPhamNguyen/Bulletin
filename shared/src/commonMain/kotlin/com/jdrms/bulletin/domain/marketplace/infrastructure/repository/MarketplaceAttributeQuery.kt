package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCapacityRange
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder

/**
 * Server-side predicates for typed attribute filters. Column names come from this fixed allowlist, never from
 * UI text. They are generated columns over `listings.attributes` (see the attributes migration); a listing with
 * no value has a NULL column, which no comparison matches, so listings missing a filtered attribute are excluded.
 */
private const val ELECTRONICS_CATEGORY = "ELECTRONICS"

internal object MarketplaceAttributeColumns {
    const val DEVICE_TYPE = "attr_device_type"
    const val BRAND_KEY = "attr_brand_key"
    const val RAM_GB = "attr_ram_gb"
    const val STORAGE_GB = "attr_storage_gb"
}

internal fun PostgrestFilterBuilder.applyAttributeFilters(filters: MarketplaceAttributeFilters) {
    if (filters.isEmpty) return
    // Exact category match (the request already guarantees Electronics) lets the partial indexes apply and keeps
    // stale attributes left behind by an older client's category change out of the results.
    eq("category", ELECTRONICS_CATEGORY)
    filters.deviceType?.let { eq(MarketplaceAttributeColumns.DEVICE_TYPE, it) }
    filters.brandKey?.let { eq(MarketplaceAttributeColumns.BRAND_KEY, it) }
    filters.ramGb?.let { applyRange(MarketplaceAttributeColumns.RAM_GB, it) }
    filters.storageGb?.let { applyRange(MarketplaceAttributeColumns.STORAGE_GB, it) }
}

private fun PostgrestFilterBuilder.applyRange(column: String, range: MarketplaceCapacityRange) {
    range.min?.let { gte(column, it) }
    range.max?.let { lte(column, it) }
}
