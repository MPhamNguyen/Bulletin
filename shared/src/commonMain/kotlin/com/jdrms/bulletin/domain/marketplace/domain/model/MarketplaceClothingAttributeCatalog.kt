package com.jdrms.bulletin.domain.marketplace.domain.model

internal object MarketplaceClothingAttributeCatalog {
    private val alphaSizes = labels("XXS", "XS", "S", "M", "L", "XL", "2XL", "3XL", "4XL")
    private val waistSizes = (24..50).associate { size -> size.toString() to size.toString() }
    private val usShoeSizes = halfSizes(4, 16)
    private val euShoeSizes = (35..50).associate { size -> size.toString() to size.toString() }
    private val accessorySizes = linkedMapOf("ONE_SIZE" to "One size", "S" to "S", "M" to "M", "L" to "L")
    private val clothingTypes = labels("top", "bottom", "outerwear", "dress", "shoes", "accessory", "other")
    private val shoeSizeSystems = linkedMapOf("us_men" to "US Men", "us_women" to "US Women", "eu" to "EU")
    private val colors = labels(
        "black", "white", "gray", "red", "orange", "yellow", "green", "blue", "purple", "pink",
        "brown", "beige", "multicolor", "other"
    )

    private val sizeGroups = listOf(
        group(mapOf(MarketplaceAttributeCatalog.CLOTHING_TYPE to setOf("top", "outerwear", "dress")), alphaSizes),
        group(mapOf(MarketplaceAttributeCatalog.CLOTHING_TYPE to setOf("bottom")), alphaSizes + waistSizes),
        group(mapOf(MarketplaceAttributeCatalog.CLOTHING_TYPE to setOf("accessory")), accessorySizes),
        group(mapOf(MarketplaceAttributeCatalog.CLOTHING_TYPE to setOf("other")), alphaSizes + accessorySizes),
        group(shoeRequirements("us_men"), usShoeSizes),
        group(shoeRequirements("us_women"), usShoeSizes),
        group(shoeRequirements("eu"), euShoeSizes)
    )

    private val allSizes = buildMap { sizeGroups.forEach { putAll(it.choices) } }

    val specs = listOf(
        MarketplaceAttributeSpec(
            MarketplaceAttributeCatalog.CLOTHING_TYPE,
            "Clothing type",
            MarketplaceAttributeKind.CHOICE,
            choices = clothingTypes
        ),
        MarketplaceAttributeSpec(
            MarketplaceAttributeCatalog.SHOE_SIZE_SYSTEM,
            "Shoe size system",
            MarketplaceAttributeKind.CHOICE,
            choices = shoeSizeSystems,
            applicability = MarketplaceAttributeApplicability(
                MarketplaceAttributeCatalog.CLOTHING_TYPE,
                setOf("shoes")
            )
        ),
        MarketplaceAttributeSpec(
            MarketplaceAttributeCatalog.SIZE,
            "Size",
            MarketplaceAttributeKind.CHOICE,
            choices = allSizes,
            applicability = MarketplaceAttributeApplicability(
                MarketplaceAttributeCatalog.CLOTHING_TYPE,
                clothingTypes.keys
            ),
            optionGroups = sizeGroups
        ),
        MarketplaceAttributeSpec(
            MarketplaceAttributeCatalog.COLOR,
            "Color",
            MarketplaceAttributeKind.CHOICE,
            choices = colors
        ),
        MarketplaceAttributeSpec(
            MarketplaceAttributeCatalog.BRAND,
            "Brand",
            MarketplaceAttributeKind.TEXT
        )
    )

    private fun shoeRequirements(system: String) = mapOf(
        MarketplaceAttributeCatalog.CLOTHING_TYPE to setOf("shoes"),
        MarketplaceAttributeCatalog.SHOE_SIZE_SYSTEM to setOf(system)
    )

    private fun group(
        requiredChoices: Map<String, Set<String>>,
        choices: Map<String, String>
    ) = MarketplaceAttributeOptionGroup(requiredChoices, choices)

    private fun halfSizes(min: Int, max: Int): Map<String, String> =
        (min * 2..max * 2).associate { half ->
            val code = if (half % 2 == 0) (half / 2).toString() else "${half / 2}.5"
            code to code
        }

    private fun labels(vararg codes: String): Map<String, String> =
        codes.associateWith { code -> code.replaceFirstChar { it.uppercase() } }
}
