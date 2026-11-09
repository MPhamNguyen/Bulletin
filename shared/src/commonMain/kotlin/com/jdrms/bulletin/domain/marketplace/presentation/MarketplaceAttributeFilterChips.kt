package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeCatalog
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributePredicate
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCapacityRange

@Composable
internal fun ActiveAttributeFilterChips(
    filters: MarketplaceFilterState,
    onFiltersApplied: (MarketplaceFilterState) -> Unit
) {
    val attributes = filters.attributes
    attributes.allPredicates.forEach { (key, predicate) ->
        val spec = filters.category?.let { MarketplaceAttributeCatalog.spec(it, key) }
        val text = listOfNotNull(spec?.label, predicate.describe(spec?.choices.orEmpty())).joinToString(": ")
        AttributeChip(text) {
            onFiltersApplied(filters.copy(attributes = attributes.without(key)))
        }
    }
}

@Composable
private fun AttributeChip(text: String, onClear: () -> Unit) {
    FilterChip(selected = true, onClick = onClear, label = { Text("$text ×") })
}

private fun MarketplaceAttributePredicate.describe(choices: Map<String, String>): String = when (this) {
    is MarketplaceAttributePredicate.ExactText -> value
    is MarketplaceAttributePredicate.Choice -> choices[code] ?: code
    is MarketplaceAttributePredicate.IntegerRange -> range.describe()
    is MarketplaceAttributePredicate.DecimalRange -> describeRange(min, max)
    is MarketplaceAttributePredicate.DateRange -> describeRange(minIso, maxIso)
}

internal fun MarketplaceCapacityRange.describe(): String = describeRange(min, max)

private fun describeRange(min: Any?, max: Any?): String = when {
    min != null && min == max -> min.toString()
    min != null && max != null -> "$min–$max"
    min != null -> "$min+"
    else -> "up to $max"
}

internal val MarketplaceAttributeFilters.activeCount: Int
    get() = allPredicates.size
