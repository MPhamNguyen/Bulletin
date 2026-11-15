package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeCatalog
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDefinition
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeKeys
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.service.ListingAttributeInputParser

internal data class ListingAttributeFormField(
    val definition: ListingAttributeDefinition,
    val enabled: Boolean,
    val unavailableReason: String? = null
)

/** Capacity fields stay visible while their device requirement is unmet; other dependent fields stay hidden. */
internal fun listingAttributeFormFields(
    category: ListingCategory,
    inputs: Map<String, String>
): List<ListingAttributeFormField> {
    val applicable = ListingAttributeInputParser().applicableDefinitions(category, inputs).map { it.key }.toSet()
    return ListingAttributeCatalog.definitionsFor(category)
        .filterNot { it.key == ListingAttributeKeys.MONTHLY_RENT_USD }
        .mapNotNull { definition ->
            val enabled = definition.key in applicable
            val capacity = definition.key == ListingAttributeKeys.RAM_GB ||
                definition.key == ListingAttributeKeys.STORAGE_GB
            if (enabled || capacity) {
                ListingAttributeFormField(
                    definition,
                    enabled,
                    if (enabled) null else unavailableReason(definition, inputs)
                )
            } else {
                null
            }
        }
}

private fun unavailableReason(definition: ListingAttributeDefinition, inputs: Map<String, String>): String {
    val prerequisite = definition.applicability
    val selected = prerequisite?.let { inputs[it.controllingKey] }
    val device = ListingAttributeCatalog.deviceTypes.firstOrNull { it.code == selected }
    return if (device == null) {
        "Select a device type to choose ${definition.label}."
    } else {
        "${definition.label} isn't available for ${device.label.lowercase()}."
    }
}
