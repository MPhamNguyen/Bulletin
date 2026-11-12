package com.jdrms.bulletin.domain.listings.domain.model

/** Attribute definitions for electronics, including single-select capacity options. */
internal object ListingElectronicsAttributeCatalog {
    private const val BRAND_MAX_LENGTH = 40
    private const val RAM_MAX_GB = 4_096L
    private const val STORAGE_MAX_GB = 100_000L

    private val ramDeviceTypes = setOf("laptop", "desktop", "tablet", "phone")
    private val storageDeviceTypes = ramDeviceTypes + "console"

    private val brand = ListingAttributeDefinition(
        ListingAttributeKeys.BRAND,
        "Brand",
        ListingAttributeType.Text(ListingTextFormat.BRAND, BRAND_MAX_LENGTH)
    )

    val definitions = listOf(
        brand,
        ListingAttributeDefinition(
            ListingAttributeKeys.DEVICE_TYPE,
            "Device type",
            ListingAttributeType.Choice(ListingAttributeCatalog.deviceTypes)
        ),
        ListingAttributeDefinition(
            ListingAttributeKeys.RAM_GB,
            "RAM",
            ListingAttributeType.Integer(
                1,
                RAM_MAX_GB,
                ListingAttributeUnit.GIGABYTES,
                ListingCapacityOptions.ram.toSet()
            ),
            ListingAttributeApplicability(ListingAttributeKeys.DEVICE_TYPE, ramDeviceTypes)
        ),
        ListingAttributeDefinition(
            ListingAttributeKeys.STORAGE_GB,
            "Storage",
            ListingAttributeType.Integer(
                1,
                STORAGE_MAX_GB,
                ListingAttributeUnit.GIGABYTES,
                ListingCapacityOptions.storage.toSet()
            ),
            ListingAttributeApplicability(ListingAttributeKeys.DEVICE_TYPE, storageDeviceTypes)
        )
    )
}
