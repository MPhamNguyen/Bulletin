package com.jdrms.bulletin.domain.marketplace.domain.model

enum class MarketplaceAttributeKind { TEXT, CHOICE, INTEGER, DECIMAL, DATE }

data class MarketplaceAttributeSpec(
    val key: String,
    val label: String,
    val kind: MarketplaceAttributeKind,
    val choices: Map<String, String> = emptyMap(),
    val prefix: String = "",
    val suffix: String = ""
)

/**
 * Marketplace's own read vocabulary for item attributes: stable key, label, kind, and unit for display and
 * filtering. The keys are the shared contract with the listings context; a cross-context test keeps them equal.
 */
object MarketplaceAttributeCatalog {
    const val BRAND = "brand"
    const val DEVICE_TYPE = "device_type"
    const val RAM_GB = "ram_gb"
    const val STORAGE_GB = "storage_gb"
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
            spec(RAM_GB, "RAM", MarketplaceAttributeKind.INTEGER, suffix = " GB"),
            spec(STORAGE_GB, "Storage", MarketplaceAttributeKind.INTEGER, suffix = " GB")
        ),
        MarketplaceCategory.TEXTBOOKS to listOf(
            spec("isbn", "ISBN", MarketplaceAttributeKind.TEXT),
            spec("course_code", "Course code", MarketplaceAttributeKind.TEXT),
            spec("edition", "Edition", MarketplaceAttributeKind.INTEGER)
        ),
        MarketplaceCategory.FURNITURE to listOf(
            spec(
                "furniture_type", "Furniture type", MarketplaceAttributeKind.CHOICE,
                choices = labels(
                    "chair", "desk", "table", "bed", "sofa", "shelf", "dresser", "lamp", "other"
                )
            ),
            spec(
                "material", "Material", MarketplaceAttributeKind.CHOICE,
                choices = labels("wood", "metal", "plastic", "glass", "fabric", "leather", "mixed", "other")
            ),
            spec("width_cm", "Width", MarketplaceAttributeKind.DECIMAL, suffix = " cm")
        ),
        MarketplaceCategory.CLOTHING to listOf(
            spec(
                "clothing_type", "Clothing type", MarketplaceAttributeKind.CHOICE,
                choices = labels("top", "bottom", "outerwear", "dress", "shoes", "accessory", "other")
            ),
            spec("size", "Size", MarketplaceAttributeKind.TEXT),
            brand
        ),
        MarketplaceCategory.HOUSING to listOf(
            spec("monthly_rent_usd", "Monthly rent", MarketplaceAttributeKind.DECIMAL, prefix = "$", suffix = "/mo"),
            spec("available_date", "Available date", MarketplaceAttributeKind.DATE),
            spec(
                "housing_type", "Housing type", MarketplaceAttributeKind.CHOICE,
                choices = linkedMapOf(
                    "room" to "Private room", "shared_room" to "Shared room", "studio" to "Studio",
                    "apartment" to "Apartment", "house" to "House", "other" to "Other"
                )
            )
        ),
        MarketplaceCategory.OTHER to emptyList()
    )

    fun specsFor(category: MarketplaceCategory): List<MarketplaceAttributeSpec> = byCategory.getValue(category)

    val allKeys: Set<String> = byCategory.values.flatten().map { it.key }.toSet()

    private fun spec(
        key: String,
        label: String,
        kind: MarketplaceAttributeKind,
        choices: Map<String, String> = emptyMap(),
        prefix: String = "",
        suffix: String = ""
    ) = MarketplaceAttributeSpec(key, label, kind, choices, prefix, suffix)

    private fun labels(vararg codes: String): Map<String, String> =
        codes.associateWith { code -> code.replaceFirstChar { it.uppercase() } }
}
