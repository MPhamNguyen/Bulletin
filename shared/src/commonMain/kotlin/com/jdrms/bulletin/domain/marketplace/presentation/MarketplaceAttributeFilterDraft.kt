package com.jdrms.bulletin.domain.marketplace.presentation

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeCatalog
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeFilters
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeMatching
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCapacityRange

/** Form text for the Electronics attribute filters. Parsing and validation live here, not in the composable. */
internal data class MarketplaceAttributeFilterDraft(
    val deviceType: String? = null,
    val brand: String = "",
    val ramMin: String = "",
    val ramMax: String = "",
    val storageMin: String = "",
    val storageMax: String = ""
) {
    data class Result(val filters: MarketplaceAttributeFilters?, val errors: Map<String, String>)

    fun toFilters(): Result {
        val errors = mutableMapOf<String, String>()
        if (brand.isNotBlank() && MarketplaceAttributeMatching.brandKey(brand).let {
                it.isEmpty() || it.length > MarketplaceAttributeCatalog.MAX_BRAND_LENGTH
            }
        ) {
            errors[BRAND] = "Enter a brand of up to ${MarketplaceAttributeCatalog.MAX_BRAND_LENGTH} characters."
        }
        val ram = range(RAM, ramMin, ramMax, errors)
        val storage = range(STORAGE, storageMin, storageMax, errors)
        if (errors.isNotEmpty()) return Result(null, errors)
        return Result(MarketplaceAttributeFilters.create(deviceType, brand, ram, storage), emptyMap())
    }

    private fun range(
        key: String,
        min: String,
        max: String,
        errors: MutableMap<String, String>
    ): MarketplaceCapacityRange? {
        if (min.isBlank() && max.isBlank()) return null
        val low = min.trim().takeIf(String::isNotEmpty)?.let { it.toLongOrNull() ?: return invalid(key, errors) }
        val high = max.trim().takeIf(String::isNotEmpty)?.let { it.toLongOrNull() ?: return invalid(key, errors) }
        return runCatching { MarketplaceCapacityRange(low, high) }.getOrElse { invalid(key, errors) }
    }

    private fun invalid(key: String, errors: MutableMap<String, String>): MarketplaceCapacityRange? {
        errors[key] = "Enter whole numbers in GB, with the minimum no larger than the maximum."
        return null
    }

    companion object {
        const val BRAND = "brand"
        const val RAM = "ram"
        const val STORAGE = "storage"

        fun from(filters: MarketplaceAttributeFilters) = MarketplaceAttributeFilterDraft(
            deviceType = filters.deviceType,
            brand = filters.brandKey.orEmpty(),
            ramMin = filters.ramGb?.min?.toString().orEmpty(),
            ramMax = filters.ramGb?.max?.toString().orEmpty(),
            storageMin = filters.storageGb?.min?.toString().orEmpty(),
            storageMax = filters.storageGb?.max?.toString().orEmpty()
        )
    }
}
