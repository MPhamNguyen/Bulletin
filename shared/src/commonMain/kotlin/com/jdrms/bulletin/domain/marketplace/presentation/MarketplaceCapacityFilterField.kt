package com.jdrms.bulletin.domain.marketplace.presentation

import androidx.compose.runtime.Composable
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeSpec
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCapacityOptions
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCapacityRange

/** Multi-select capacity chips with an inclusive, open-ended final option. */
@Composable
internal fun MarketplaceCapacityFilterField(
    spec: MarketplaceAttributeSpec,
    input: MarketplaceAttributeFilterInput,
    onToggle: (MarketplaceCapacityRange) -> Unit
) {
    val options = MarketplaceCapacityOptions.forKey(spec.key)
    MarketplaceFilterSection(
        title = spec.label,
        hint = "${MarketplaceCapacityOptions.label(options.last())} includes larger sizes."
    ) {
        MarketplacePillGroup(
            options = options,
            isSelected = { it in input.selectedCapacities(spec.key) },
            label = MarketplaceCapacityOptions::label,
            onToggle = onToggle
        )
    }
}

/** Recognize a previously applied exact capacity when opening the new chip UI. */
internal fun MarketplaceAttributeFilterInput.selectedCapacities(key: String): Set<MarketplaceCapacityRange> {
    if (capacities.isNotEmpty()) return capacities
    return MarketplaceCapacityOptions.forKey(key).filter { range ->
        (exact.toLongOrNull() == range.min && range.min == range.max) ||
            (min.toLongOrNull() == range.min && max.toLongOrNull() == range.max && min.isNotBlank())
    }.toSet()
}
