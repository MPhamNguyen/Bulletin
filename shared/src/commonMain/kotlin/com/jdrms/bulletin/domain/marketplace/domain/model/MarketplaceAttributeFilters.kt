package com.jdrms.bulletin.domain.marketplace.domain.model

/** Inclusive whole-number range. Either bound may be open, but not both. */
data class MarketplaceCapacityRange(val min: Long? = null, val max: Long? = null) {
    init {
        require(min != null || max != null) { "A capacity range needs a minimum or a maximum." }
        require((min ?: 0L) in 0..MarketplaceAttributeCatalog.MAX_CAPACITY_GB) { "Minimum capacity is out of range." }
        require((max ?: 0L) in 0..MarketplaceAttributeCatalog.MAX_CAPACITY_GB) { "Maximum capacity is out of range." }
        require(min == null || max == null || min <= max) { "Minimum capacity cannot exceed the maximum." }
    }

    fun contains(value: Long): Boolean = (min == null || value >= min) && (max == null || value <= max)
}

/**
 * Typed buyer filters over item attributes. Only the allowlisted fields below exist, so no raw JSON path or
 * query fragment can be expressed. A listing missing a filtered attribute does not match.
 *
 * [brandKey] is the canonical matching key (see [MarketplaceAttributeMatching.brandKey]), never display text, so
 * equal filters are equal values and share a cache entry. Build one from user text with [create].
 */
data class MarketplaceAttributeFilters(
    val deviceType: String? = null,
    val brandKey: String? = null,
    val ramGb: MarketplaceCapacityRange? = null,
    val storageGb: MarketplaceCapacityRange? = null
) {
    init {
        require(deviceType == null || deviceType in MarketplaceAttributeCatalog.deviceTypes) {
            "Unsupported device type filter."
        }
        require(brandKey == null || brandKey == MarketplaceAttributeMatching.brandKey(brandKey)) {
            "Brand filter must be a normalized matching key."
        }
        require(
            brandKey == null ||
                (brandKey.isNotEmpty() && brandKey.length <= MarketplaceAttributeCatalog.MAX_BRAND_LENGTH)
        ) {
            "Brand filter must be between 1 and ${MarketplaceAttributeCatalog.MAX_BRAND_LENGTH} characters."
        }
    }

    val isEmpty: Boolean get() = this == NONE

    fun matches(attributes: MarketplaceAttributes): Boolean =
        matchesChoice(attributes) && matchesBrand(attributes) &&
            matchesRange(ramGb, attributes[MarketplaceAttributeCatalog.RAM_GB]) &&
            matchesRange(storageGb, attributes[MarketplaceAttributeCatalog.STORAGE_GB])

    private fun matchesChoice(attributes: MarketplaceAttributes): Boolean =
        deviceType == null ||
            (attributes[MarketplaceAttributeCatalog.DEVICE_TYPE] as? MarketplaceAttributeValue.Choice)?.code ==
            deviceType

    private fun matchesBrand(attributes: MarketplaceAttributes): Boolean =
        brandKey == null || (attributes[MarketplaceAttributeCatalog.BRAND] as? MarketplaceAttributeValue.Text)
            ?.let { MarketplaceAttributeMatching.brandKey(it.value) == brandKey } == true

    private fun matchesRange(range: MarketplaceCapacityRange?, value: MarketplaceAttributeValue?): Boolean =
        range == null || (value as? MarketplaceAttributeValue.Integer)?.let { range.contains(it.value) } == true

    companion object {
        val NONE = MarketplaceAttributeFilters()

        /** Normalizes user brand text; blank text means no brand filter. */
        fun create(
            deviceType: String? = null,
            brandText: String? = null,
            ramGb: MarketplaceCapacityRange? = null,
            storageGb: MarketplaceCapacityRange? = null
        ) = MarketplaceAttributeFilters(
            deviceType = deviceType,
            brandKey = brandText?.let(MarketplaceAttributeMatching::brandKey)?.takeIf(String::isNotEmpty),
            ramGb = ramGb,
            storageGb = storageGb
        )
    }
}

object MarketplaceAttributeMatching {
    private val WHITESPACE = Regex("\\s+")

    /** Same rules as the listings context and the SQL `listing_text_match_key` function; tests keep them equal. */
    fun brandKey(text: String): String {
        val spaced = text.lowercase()
            .replace("&", " and ")
            .replace(Regex("[-_/]"), " ")
            .replace(Regex("['’.,]"), "")
        return spaced.filter { it.isLetterOrDigit() || it == ' ' }.trim().replace(WHITESPACE, " ")
    }
}
