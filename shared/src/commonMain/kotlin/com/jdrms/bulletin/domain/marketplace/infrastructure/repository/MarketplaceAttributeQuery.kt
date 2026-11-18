package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeCatalog
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributePredicate
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCapacityRange
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import io.github.jan.supabase.postgrest.query.filter.PostgrestFilterBuilder

/**
 * Server-side predicates for typed attribute filters. Every attribute lives in the `attributes` jsonb column and is
 * addressed by a JSON path built from a catalog key, never from UI text. The one exception is [BRAND_KEY], a
 * computed column (`public.listing_brand_key(listings)`) that derives the normalized brand match key from
 * `attributes->>'brand'`. A listing with no value has a NULL result, which no comparison matches, so listings
 * missing a filtered attribute are excluded.
 */
internal object MarketplaceAttributeColumns {
    const val BRAND_KEY = "listing_brand_key"
}

internal fun PostgrestFilterBuilder.applyAttributeFilters(
    filters: MarketplaceAttributeFilters,
    category: MarketplaceCategory?
) {
    if (filters.isEmpty) return
    // Exact category match lets partial indexes apply and keeps
    // stale attributes left behind by an older client's category change out of the results.
    eq("category", requireNotNull(category).name)
    val (options, other) = filters.allPredicates.entries.partition {
        it.value is MarketplaceAttributePredicate.IntegerOptions
    }
    other.forEach { (key, predicate) -> applyAttributePredicate(key, predicate) }
    and { options.forEach { (key, predicate) -> applyAttributePredicate(key, predicate) } }
}

private fun PostgrestFilterBuilder.applyAttributePredicate(
    key: String,
    predicate: MarketplaceAttributePredicate
) {
    val textColumn = MarketplaceAttributeColumns.textColumn(key, predicate)
    when (predicate) {
        is MarketplaceAttributePredicate.ChoiceOptions -> isIn(textColumn, predicate.codes.sorted())
        is MarketplaceAttributePredicate.Choice -> eq(textColumn, predicate.code)
        is MarketplaceAttributePredicate.ExactText -> {
            eq(textColumn, predicate.value)
        }
        is MarketplaceAttributePredicate.IntegerRange -> applyRange(
            numberColumn(key),
            predicate.range
        )
        is MarketplaceAttributePredicate.IntegerOptions -> {
            val column = numberColumn(key)
            or {
                predicate.ranges.sortedBy { it.min }.forEach { range ->
                    if (range.min == range.max) {
                        eq(column, requireNotNull(range.min))
                    } else {
                        and { applyRange(column, range) }
                    }
                }
            }
        }
        is MarketplaceAttributePredicate.DecimalRange -> {
            val column = numberColumn(key)
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
    key == MarketplaceAttributeCatalog.BRAND && predicate is MarketplaceAttributePredicate.ExactText &&
        predicate.usesMatchKey -> BRAND_KEY
    else -> "attributes->>$key"
}

private fun numberColumn(key: String): String = "attributes->$key"

private fun PostgrestFilterBuilder.applyRange(column: String, range: MarketplaceCapacityRange) {
    range.min?.let { gte(column, it) }
    range.max?.let { lte(column, it) }
}
