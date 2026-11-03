package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeCatalog
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCapacityRange

/** Active attribute filters as removable chips; tapping one clears just that filter. */
@Composable
internal fun ActiveAttributeFilterChips(
    filters: MarketplaceFilterState,
    onFiltersApplied: (MarketplaceFilterState) -> Unit
) {
    val attributes = filters.attributes
    fun clear(update: MarketplaceAttributeFilters) = onFiltersApplied(filters.copy(attributes = update))
    attributes.deviceType?.let { code ->
        AttributeChip(
            MarketplaceAttributeCatalog.deviceTypes[code] ?: code
        ) { clear(attributes.copy(deviceType = null)) }
    }
    attributes.brandKey?.let { key -> AttributeChip("Brand: $key") { clear(attributes.copy(brandKey = null)) } }
    attributes.ramGb?.let { range -> AttributeChip("RAM ${range.describe()}") { clear(attributes.copy(ramGb = null)) } }
    attributes.storageGb?.let { range ->
        AttributeChip("Storage ${range.describe()}") { clear(attributes.copy(storageGb = null)) }
    }
}

@Composable
private fun AttributeChip(text: String, onClear: () -> Unit) {
    FilterChip(selected = true, onClick = onClear, label = { Text("$text ×") })
}

internal fun MarketplaceCapacityRange.describe(): String = when {
    min != null && max != null -> "$min–$max GB"
    min != null -> "$min+ GB"
    else -> "up to $max GB"
}

internal val MarketplaceAttributeFilters.activeCount: Int
    get() = listOfNotNull(deviceType, brandKey, ramGb, storageGb).size
