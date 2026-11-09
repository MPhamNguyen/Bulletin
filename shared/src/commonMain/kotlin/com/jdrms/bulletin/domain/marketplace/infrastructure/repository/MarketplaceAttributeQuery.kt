package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeCatalog
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributePredicate
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCapacityRange
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder

/**
 * Server-side predicates for typed attribute filters. Column and JSON-path names come from this fixed allowlist,
 * never from UI text. A listing with no value has a NULL result, which no comparison matches, so listings missing
 * a filtered attribute are excluded.
 */
internal object MarketplaceAttributeColumns {
    const val DEVICE_TYPE = "attr_device_type"
    const val BRAND_KEY = "attr_brand_key"
    const val RAM_GB = "attr_ram_gb"
    const val STORAGE_GB = "attr_storage_gb"
}

internal fun PostgrestFilterBuilder.applyAttributeFilters(
    filters: MarketplaceAttributeFilters,
    category: MarketplaceCategory?
) {
    if (filters.isEmpty) return
    // Exact category match lets partial indexes apply and keeps
    // stale attributes left behind by an older client's category change out of the results.
    eq("category", requireNotNull(category).name)
    filters.allPredicates.forEach { (key, predicate) -> applyAttributePredicate(key, predicate) }
}

private fun PostgrestFilterBuilder.applyAttributePredicate(
    key: String,
    predicate: MarketplaceAttributePredicate
) {
    val textColumn = MarketplaceAttributeColumns.textColumn(key, predicate)
    when (predicate) {
        is MarketplaceAttributePredicate.Choice -> eq(textColumn, predicate.code)
        is MarketplaceAttributePredicate.ExactText -> {
            eq(textColumn, predicate.value)
        }
        is MarketplaceAttributePredicate.IntegerRange -> applyRange(
            MarketplaceAttributeColumns.numberColumn(key),
            predicate.range
        )
        is MarketplaceAttributePredicate.DecimalRange -> {
            val column = MarketplaceAttributeColumns.numberColumn(key)
            predicate.min?.let { gte(column, it) }
            predicate.max?.let { lte(column, it) }
        }
        is MarketplaceAttributePredicate.DateRange -> {
            predicate.minIso?.let { gte(textColumn, it) }
            predicate.maxIso?.let { lte(textColumn, it) }
        }
    }
}

private fun MarketplaceAttributeColumns.textColumn(
    key: String,
    predicate: MarketplaceAttributePredicate
): String = when {
    key == MarketplaceAttributeCatalog.DEVICE_TYPE -> DEVICE_TYPE
    key == MarketplaceAttributeCatalog.BRAND && predicate is MarketplaceAttributePredicate.ExactText &&
        predicate.usesMatchKey -> BRAND_KEY
    else -> "attributes->>$key"
}

private fun MarketplaceAttributeColumns.numberColumn(key: String): String = when (key) {
    MarketplaceAttributeCatalog.RAM_GB -> RAM_GB
    MarketplaceAttributeCatalog.STORAGE_GB -> STORAGE_GB
    else -> "attributes->$key"
}

private fun PostgrestFilterBuilder.applyRange(column: String, range: MarketplaceCapacityRange) {
    range.min?.let { gte(column, it) }
    range.max?.let { lte(column, it) }
}
