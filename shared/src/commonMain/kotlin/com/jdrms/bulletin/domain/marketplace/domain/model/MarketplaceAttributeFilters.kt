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

/** Buyer filters over the same typed, category-owned attributes sellers enter when posting. */
data class MarketplaceAttributeFilters(
    val deviceType: String? = null,
    val brandKey: String? = null,
    val ramGb: MarketplaceCapacityRange? = null,
    val storageGb: MarketplaceCapacityRange? = null,
    val predicates: Map<String, MarketplaceAttributePredicate> = emptyMap()
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
                brandKey.isNotEmpty() && brandKey.length <= MarketplaceAttributeCatalog.MAX_BRAND_LENGTH
        ) {
            "Brand filter must be between 1 and ${MarketplaceAttributeCatalog.MAX_BRAND_LENGTH} characters."
        }
        require(predicates.keys.all { it in MarketplaceAttributeCatalog.allKeys }) {
            "Unsupported marketplace attribute filter."
        }
    }

    val allPredicates: Map<String, MarketplaceAttributePredicate>
        get() = buildMap {
            deviceType?.let {
                put(
                    MarketplaceAttributeCatalog.DEVICE_TYPE,
                    MarketplaceAttributePredicate.Choice(it)
                )
            }
            brandKey?.let {
                put(
                    MarketplaceAttributeCatalog.BRAND,
                    MarketplaceAttributePredicate.ExactText(it, usesMatchKey = true)
                )
            }
            ramGb?.let {
                put(
                    MarketplaceAttributeCatalog.RAM_GB,
                    MarketplaceAttributePredicate.IntegerRange(it)
                )
            }
            storageGb?.let {
                put(
                    MarketplaceAttributeCatalog.STORAGE_GB,
                    MarketplaceAttributePredicate.IntegerRange(it)
                )
            }
            putAll(predicates)
        }

    val isEmpty: Boolean get() = allPredicates.isEmpty()

    fun isValidFor(category: MarketplaceCategory?): Boolean = reconciledFor(category) == this

    fun reconciledFor(category: MarketplaceCategory?): MarketplaceAttributeFilters {
        if (category == null) return NONE
        val selectedChoices = allPredicates.mapNotNull { (key, predicate) ->
            (predicate as? MarketplaceAttributePredicate.Choice)?.let { key to it.code }
        }.toMap()
        val validKeys = allPredicates.filter { (key, predicate) ->
            MarketplaceAttributeCatalog.spec(category, key)?.let { spec ->
                spec.isApplicable(selectedChoices) && spec.accepts(predicate, selectedChoices)
            } == true
        }.keys
        return copy(
            deviceType = deviceType.takeIf { MarketplaceAttributeCatalog.DEVICE_TYPE in validKeys },
            brandKey = brandKey.takeIf { MarketplaceAttributeCatalog.BRAND in validKeys },
            ramGb = ramGb.takeIf { MarketplaceAttributeCatalog.RAM_GB in validKeys },
            storageGb = storageGb.takeIf { MarketplaceAttributeCatalog.STORAGE_GB in validKeys },
            predicates = predicates.filterKeys(validKeys::contains)
        )
    }

    fun matches(attributes: MarketplaceAttributes): Boolean = allPredicates.all { (key, predicate) ->
        predicate.matches(attributes[key])
    }

    fun without(key: String): MarketplaceAttributeFilters = copy(
        deviceType = deviceType.takeUnless { key == MarketplaceAttributeCatalog.DEVICE_TYPE },
        brandKey = brandKey.takeUnless { key == MarketplaceAttributeCatalog.BRAND },
        ramGb = ramGb.takeUnless { key == MarketplaceAttributeCatalog.RAM_GB },
        storageGb = storageGb.takeUnless { key == MarketplaceAttributeCatalog.STORAGE_GB },
        predicates = predicates - key
    )

    companion object {
        val NONE = MarketplaceAttributeFilters()

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

private fun MarketplaceAttributeSpec.accepts(
    predicate: MarketplaceAttributePredicate,
    selectedChoices: Map<String, String>
): Boolean = when (kind) {
    MarketplaceAttributeKind.TEXT -> predicate is MarketplaceAttributePredicate.ExactText
    MarketplaceAttributeKind.CHOICE ->
        predicate is MarketplaceAttributePredicate.Choice && predicate.code in choicesFor(selectedChoices)
    MarketplaceAttributeKind.INTEGER -> predicate is MarketplaceAttributePredicate.IntegerRange
    MarketplaceAttributeKind.DECIMAL -> predicate is MarketplaceAttributePredicate.DecimalRange
    MarketplaceAttributeKind.DATE -> predicate is MarketplaceAttributePredicate.DateRange
}

private fun MarketplaceAttributePredicate.matches(value: MarketplaceAttributeValue?): Boolean = when (this) {
    is MarketplaceAttributePredicate.Choice -> (value as? MarketplaceAttributeValue.Choice)?.code == code
    is MarketplaceAttributePredicate.ExactText -> matchesText(value)
    is MarketplaceAttributePredicate.IntegerRange ->
        (value as? MarketplaceAttributeValue.Integer)?.let { range.contains(it.value) } == true
    is MarketplaceAttributePredicate.DecimalRange -> matchesDecimal(value)
    is MarketplaceAttributePredicate.DateRange -> matchesDate(value)
}

private fun MarketplaceAttributePredicate.ExactText.matchesText(value: MarketplaceAttributeValue?): Boolean {
    val text = (value as? MarketplaceAttributeValue.Text)?.value ?: return false
    return if (usesMatchKey) {
        MarketplaceAttributeMatching.brandKey(text) == this.value
    } else {
        text.equals(this.value, ignoreCase = true)
    }
}

private fun MarketplaceAttributePredicate.DecimalRange.matchesDecimal(value: MarketplaceAttributeValue?): Boolean {
    val number = (value as? MarketplaceAttributeValue.Decimal)?.plain?.toDoubleOrNull() ?: return false
    return (min == null || number >= min) && (max == null || number <= max)
}

private fun MarketplaceAttributePredicate.DateRange.matchesDate(value: MarketplaceAttributeValue?): Boolean {
    val date = (value as? MarketplaceAttributeValue.Date)?.iso ?: return false
    return (minIso == null || date >= minIso) && (maxIso == null || date <= maxIso)
}

object MarketplaceAttributeMatching {
    private val COURSE_CODE = Regex("^([A-Za-z]{2,5})[ -]?(\\d{2,4}[A-Za-z]{0,2})$")
    private val WHITESPACE = Regex("\\s+")

    fun brandKey(text: String): String {
        val spaced = text.lowercase()
            .replace("&", " and ")
            .replace(Regex("[-_/]"), " ")
            .replace(Regex("['\\u2019.,]"), "")
        return spaced.filter { it.isLetterOrDigit() || it == ' ' }.trim().replace(WHITESPACE, " ")
    }

    fun exactText(key: String, text: String): String? {
        val cleaned = text.trim().replace(WHITESPACE, " ").takeIf(String::isNotEmpty) ?: return null
        return when (key) {
            MarketplaceAttributeCatalog.BRAND -> brandKey(cleaned).takeIf(String::isNotEmpty)
            MarketplaceAttributeCatalog.ISBN -> cleaned.filterNot { it == '-' || it == ' ' }.uppercase()
            MarketplaceAttributeCatalog.COURSE_CODE -> normalizeCourseCode(cleaned)
            MarketplaceAttributeCatalog.SIZE -> cleaned.uppercase()
            else -> cleaned
        }
    }

    private fun normalizeCourseCode(text: String): String? {
        val match = COURSE_CODE.matchEntire(text) ?: return null
        val (department, number) = match.destructured
        return "${department.uppercase()} ${number.uppercase()}"
    }
}
