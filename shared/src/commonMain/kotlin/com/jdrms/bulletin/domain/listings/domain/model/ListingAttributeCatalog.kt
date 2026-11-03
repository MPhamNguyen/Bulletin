package com.jdrms.bulletin.domain.listings.domain.model

/**
 * Stable keys are part of the persisted contract: never rename or repurpose one. Retire a key by adding it to
 * [retiredKeys] so existing stored values are preserved but no longer shown, edited, or validated.
 */
object ListingAttributeKeys {
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
    const val CLOTHING_TYPE = "clothing_type"
    const val SIZE = "size"
    const val MONTHLY_RENT_USD = "monthly_rent_usd"
    const val AVAILABLE_DATE = "available_date"
    const val HOUSING_TYPE = "housing_type"
}

object ListingAttributeCatalog {
    private const val BRAND_MAX_LENGTH = 40
    private const val SIZE_MAX_LENGTH = 12
    private const val ISBN_MAX_LENGTH = 13
    private const val COURSE_CODE_MAX_LENGTH = 12
    private const val RAM_MAX_GB = 4_096L
    private const val STORAGE_MAX_GB = 100_000L
    private const val MAX_EDITION = 99L
    private const val MAX_WIDTH_CM_TENTHS = 10_000L
    private const val MAX_RENT_CENTS = 9_999_999L
    private const val MIN_DATE_YEAR = 2000
    private const val MAX_DATE_YEAR = 2100

    val retiredKeys: Set<String> = emptySet()

    val deviceTypes = options(
        "laptop" to "Laptop",
        "desktop" to "Desktop",
        "tablet" to "Tablet",
        "phone" to "Phone",
        "monitor" to "Monitor",
        "console" to "Game console",
        "accessory" to "Accessory",
        "other" to "Other"
    )

    private val ramDeviceTypes = setOf("laptop", "desktop", "tablet", "phone")
    private val storageDeviceTypes = ramDeviceTypes + "console"

    private val brand = ListingAttributeDefinition(
        ListingAttributeKeys.BRAND,
        "Brand",
        ListingAttributeType.Text(ListingTextFormat.BRAND, BRAND_MAX_LENGTH)
    )

    private val electronics = listOf(
        brand,
        ListingAttributeDefinition(
            ListingAttributeKeys.DEVICE_TYPE,
            "Device type",
            ListingAttributeType.Choice(deviceTypes)
        ),
        ListingAttributeDefinition(
            ListingAttributeKeys.RAM_GB,
            "RAM",
            ListingAttributeType.Integer(1, RAM_MAX_GB, ListingAttributeUnit.GIGABYTES),
            ListingAttributeApplicability(ListingAttributeKeys.DEVICE_TYPE, ramDeviceTypes)
        ),
        ListingAttributeDefinition(
            ListingAttributeKeys.STORAGE_GB,
            "Storage",
            ListingAttributeType.Integer(1, STORAGE_MAX_GB, ListingAttributeUnit.GIGABYTES),
            ListingAttributeApplicability(ListingAttributeKeys.DEVICE_TYPE, storageDeviceTypes)
        )
    )

    private val textbooks = listOf(
        ListingAttributeDefinition(
            ListingAttributeKeys.ISBN,
            "ISBN",
            ListingAttributeType.Text(ListingTextFormat.ISBN, ISBN_MAX_LENGTH)
        ),
        ListingAttributeDefinition(
            ListingAttributeKeys.COURSE_CODE,
            "Course code",
            ListingAttributeType.Text(ListingTextFormat.COURSE_CODE, COURSE_CODE_MAX_LENGTH)
        ),
        ListingAttributeDefinition(
            ListingAttributeKeys.EDITION,
            "Edition",
            ListingAttributeType.Integer(1, MAX_EDITION)
        )
    )

    private val furniture = listOf(
        ListingAttributeDefinition(
            ListingAttributeKeys.FURNITURE_TYPE,
            "Furniture type",
            ListingAttributeType.Choice(
                options(
                    "chair" to "Chair", "desk" to "Desk", "table" to "Table", "bed" to "Bed", "sofa" to "Sofa",
                    "shelf" to "Shelf", "dresser" to "Dresser", "lamp" to "Lamp", "other" to "Other"
                )
            )
        ),
        ListingAttributeDefinition(
            ListingAttributeKeys.MATERIAL,
            "Material",
            ListingAttributeType.Choice(
                options(
                    "wood" to "Wood",
                    "metal" to "Metal",
                    "plastic" to "Plastic",
                    "glass" to "Glass",
                    "fabric" to "Fabric",
                    "leather" to "Leather",
                    "mixed" to "Mixed",
                    "other" to "Other"
                )
            )
        ),
        ListingAttributeDefinition(
            ListingAttributeKeys.WIDTH_CM,
            "Width",
            ListingAttributeType.Decimal(
                ListingDecimal(1, 1),
                ListingDecimal(MAX_WIDTH_CM_TENTHS, 1),
                ListingAttributeUnit.CENTIMETERS
            )
        )
    )

    private val clothing = listOf(
        ListingAttributeDefinition(
            ListingAttributeKeys.CLOTHING_TYPE,
            "Clothing type",
            ListingAttributeType.Choice(
                options(
                    "top" to "Top",
                    "bottom" to "Bottom",
                    "outerwear" to "Outerwear",
                    "dress" to "Dress",
                    "shoes" to "Shoes",
                    "accessory" to "Accessory",
                    "other" to "Other"
                )
            )
        ),
        ListingAttributeDefinition(
            ListingAttributeKeys.SIZE,
            "Size",
            ListingAttributeType.Text(ListingTextFormat.CLOTHING_SIZE, SIZE_MAX_LENGTH)
        ),
        brand
    )

    private val housing = listOf(
        ListingAttributeDefinition(
            ListingAttributeKeys.MONTHLY_RENT_USD,
            "Monthly rent",
            ListingAttributeType.Decimal(
                ListingDecimal(1, 2),
                ListingDecimal(MAX_RENT_CENTS, 2),
                ListingAttributeUnit.USD_PER_MONTH
            )
        ),
        ListingAttributeDefinition(
            ListingAttributeKeys.AVAILABLE_DATE,
            "Available date",
            ListingAttributeType.Date(MIN_DATE_YEAR, MAX_DATE_YEAR)
        ),
        ListingAttributeDefinition(
            ListingAttributeKeys.HOUSING_TYPE,
            "Housing type",
            ListingAttributeType.Choice(
                options(
                    "room" to "Private room",
                    "shared_room" to "Shared room",
                    "studio" to "Studio",
                    "apartment" to "Apartment",
                    "house" to "House",
                    "other" to "Other"
                )
            )
        )
    )

    private val byCategory: Map<ListingCategory, List<ListingAttributeDefinition>> = mapOf(
        ListingCategory.ELECTRONICS to electronics,
        ListingCategory.TEXTBOOKS to textbooks,
        ListingCategory.FURNITURE to furniture,
        ListingCategory.CLOTHING to clothing,
        ListingCategory.HOUSING to housing,
        ListingCategory.OTHER to emptyList()
    )

    /** Every key any category defines; a stored key outside this set (and not applicable) is unrecognized. */
    val allKeys: Set<String> = byCategory.values.flatten().map { it.key }.toSet()

    fun definitionsFor(category: ListingCategory): List<ListingAttributeDefinition> =
        byCategory.getValue(category)

    fun definition(category: ListingCategory, key: String): ListingAttributeDefinition? =
        definitionsFor(category).firstOrNull { it.key == key }

    fun isRetired(key: String): Boolean = key in retiredKeys

    private fun options(vararg pairs: Pair<String, String>) = pairs.map { (code, label) ->
        ListingAttributeOption(code, label)
    }
}
