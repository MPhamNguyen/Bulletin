package com.jdrms.bulletin.domain.marketplace.domain.model

enum class MarketplaceAttributeKind { TEXT, CHOICE, INTEGER, DECIMAL, DATE }

enum class MarketplaceAttributeFilterMode { EXACT, RANGE }

data class MarketplaceAttributeSpec(
    val key: String,
    val label: String,
    val kind: MarketplaceAttributeKind,
    val choices: Map<String, String> = emptyMap(),
    val prefix: String = "",
    val suffix: String = "",
    val filterMode: MarketplaceAttributeFilterMode = MarketplaceAttributeFilterMode.RANGE,
    val applicability: MarketplaceAttributeApplicability? = null,
    val optionGroups: List<MarketplaceAttributeOptionGroup> = emptyList()
) {
    fun isApplicable(selectedChoices: Map<String, String>): Boolean =
        applicability?.let { selectedChoices[it.controllingKey] in it.allowedCodes } ?: true

    fun choicesFor(selectedChoices: Map<String, String>): Map<String, String> {
        if (optionGroups.isEmpty()) return choices
        return buildMap {
            optionGroups.filter { it.matches(selectedChoices) }.forEach { putAll(it.choices) }
        }
    }
}

data class MarketplaceAttributeApplicability(val controllingKey: String, val allowedCodes: Set<String>)

data class MarketplaceAttributeOptionGroup(
    val requiredChoices: Map<String, Set<String>>,
    val choices: Map<String, String>
) {
    fun matches(selectedChoices: Map<String, String>): Boolean =
        requiredChoices.all { (key, allowed) -> selectedChoices[key] in allowed }
}

/**
 * Marketplace's own read vocabulary for item attributes: stable key, label, kind, and unit for display and
 * filtering. The keys are the shared contract with the listings context; a cross-context test keeps them equal.
 */
object MarketplaceAttributeCatalog {
    const val BRAND = "brand"
    const val DEVICE_TYPE = "device_type"
    const val RAM_GB = "ram_gb"
    const val STORAGE_GB = "storage_gb"
    const val ISBN = "isbn"
    const val COURSE_CODE = "course_code"
    const val EDITION = "edition"
    const val FURNITURE_TYPE = "furniture_type"
    const val MATERIAL = "material"
    const val WIDTH_CM = "width_cm"
    const val LENGTH_CM = "length_cm"
    const val CLOTHING_TYPE = "clothing_type"
    const val SHOE_SIZE_SYSTEM = "shoe_size_system"
    const val SIZE = "size"
    const val COLOR = "color"
    const val MONTHLY_RENT_USD = "monthly_rent_usd"
    const val AVAILABLE_DATE = "available_date"
    const val HOUSING_TYPE = "housing_type"
    const val MAX_BRAND_LENGTH = 40
    const val MAX_CAPACITY_GB = 100_000L

    val deviceTypes: Map<String, String> = linkedMapOf(
        "laptop" to "Laptop",
        "desktop" to "Desktop",
        "tablet" to "Tablet",
        "phone" to "Phone",
        "monitor" to "Monitor",
        "console" to "Game console",
        "accessory" to "Accessory",
        "other" to "Other"
    )

    private val brand = spec(BRAND, "Brand", MarketplaceAttributeKind.TEXT)

    private val byCategory: Map<MarketplaceCategory, List<MarketplaceAttributeSpec>> = mapOf(
        MarketplaceCategory.ELECTRONICS to listOf(
            brand,
            spec(DEVICE_TYPE, "Device type", MarketplaceAttributeKind.CHOICE, choices = deviceTypes),
            spec(
                RAM_GB, "RAM", MarketplaceAttributeKind.INTEGER,
                suffix = " GB", filterMode = MarketplaceAttributeFilterMode.EXACT
            ),
            spec(
                STORAGE_GB, "Storage", MarketplaceAttributeKind.INTEGER,
                suffix = " GB", filterMode = MarketplaceAttributeFilterMode.EXACT
            )
        ),
        MarketplaceCategory.TEXTBOOKS to listOf(
            spec(ISBN, "ISBN", MarketplaceAttributeKind.TEXT),
            spec(COURSE_CODE, "Course code", MarketplaceAttributeKind.TEXT),
            spec(
                EDITION, "Edition", MarketplaceAttributeKind.INTEGER,
                filterMode = MarketplaceAttributeFilterMode.EXACT
            )
        ),
        MarketplaceCategory.FURNITURE to listOf(
            spec(
                FURNITURE_TYPE, "Furniture type", MarketplaceAttributeKind.CHOICE,
                choices = labels(
                    "chair", "desk", "table", "bed", "sofa", "shelf", "dresser", "lamp", "other"
                )
            ),
            spec(
                MATERIAL, "Material", MarketplaceAttributeKind.CHOICE,
                choices = labels("wood", "metal", "plastic", "glass", "fabric", "leather", "mixed", "other")
            ),
            spec(WIDTH_CM, "Width", MarketplaceAttributeKind.DECIMAL, suffix = " cm"),
            spec(LENGTH_CM, "Length", MarketplaceAttributeKind.DECIMAL, suffix = " cm")
        ),
        MarketplaceCategory.CLOTHING to MarketplaceClothingAttributeCatalog.specs,
        MarketplaceCategory.HOUSING to listOf(
            spec(MONTHLY_RENT_USD, "Monthly rent", MarketplaceAttributeKind.DECIMAL, prefix = "$", suffix = "/mo"),
            spec(AVAILABLE_DATE, "Available date", MarketplaceAttributeKind.DATE),
            spec(
                HOUSING_TYPE, "Housing type", MarketplaceAttributeKind.CHOICE,
                choices = linkedMapOf(
                    "room" to "Private room", "shared_room" to "Shared room", "studio" to "Studio",
                    "apartment" to "Apartment", "house" to "House", "other" to "Other"
                )
            )
        ),
        MarketplaceCategory.OTHER to emptyList()
    )

    fun specsFor(category: MarketplaceCategory): List<MarketplaceAttributeSpec> = byCategory.getValue(category)

    fun spec(category: MarketplaceCategory, key: String): MarketplaceAttributeSpec? =
        specsFor(category).firstOrNull { it.key == key }

    fun applicableSpecs(
        category: MarketplaceCategory,
        selectedChoices: Map<String, String>
    ): List<MarketplaceAttributeSpec> = specsFor(category).filter { spec ->
        spec.isApplicable(selectedChoices) &&
            (spec.optionGroups.isEmpty() || spec.choicesFor(selectedChoices).isNotEmpty())
    }

    val allKeys: Set<String> = byCategory.values.flatten().map { it.key }.toSet()

    private fun spec(
        key: String,
        label: String,
        kind: MarketplaceAttributeKind,
        choices: Map<String, String> = emptyMap(),
        prefix: String = "",
        suffix: String = "",
        filterMode: MarketplaceAttributeFilterMode = MarketplaceAttributeFilterMode.RANGE
    ) = MarketplaceAttributeSpec(key, label, kind, choices, prefix, suffix, filterMode)

    private fun labels(vararg codes: String): Map<String, String> =
        codes.associateWith { code -> code.replaceFirstChar { it.uppercase() } }
}
