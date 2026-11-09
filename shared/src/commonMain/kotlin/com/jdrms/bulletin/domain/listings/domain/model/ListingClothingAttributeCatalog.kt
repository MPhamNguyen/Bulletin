package com.jdrms.bulletin.domain.listings.domain.model

internal object ListingClothingAttributeCatalog {
    private const val BRAND_MAX_LENGTH = 40
    private val alphaSizes = options("XXS", "XS", "S", "M", "L", "XL", "2XL", "3XL", "4XL")
    private val waistSizes = (24..50).map { size -> ListingAttributeOption(size.toString(), size.toString()) }
    private val usShoeSizes = halfSizes(4, 16)
    private val euShoeSizes = (35..50).map { size -> ListingAttributeOption(size.toString(), size.toString()) }
    private val accessorySizes = options("ONE_SIZE" to "One size", "S" to "S", "M" to "M", "L" to "L")

    private val clothingTypes = listOf(
        ListingAttributeOption("top", "Top"),
        ListingAttributeOption("bottom", "Bottom"),
        ListingAttributeOption("outerwear", "Outerwear"),
        ListingAttributeOption("dress", "Dress"),
        ListingAttributeOption("shoes", "Shoes"),
        ListingAttributeOption("accessory", "Accessory"),
        ListingAttributeOption("other", "Other")
    )

    private val shoeSizeSystems = listOf(
        ListingAttributeOption("us_men", "US Men"),
        ListingAttributeOption("us_women", "US Women"),
        ListingAttributeOption("eu", "EU")
    )

    private val colorOptions = listOf(
        "black", "white", "gray", "red", "orange", "yellow", "green", "blue", "purple", "pink",
        "brown", "beige", "multicolor", "other"
    ).map { code -> ListingAttributeOption(code, code.replaceFirstChar { it.uppercase() }) }

    private val sizeGroups = listOf(
        group(mapOf(ListingAttributeKeys.CLOTHING_TYPE to setOf("top", "outerwear", "dress")), alphaSizes),
        group(mapOf(ListingAttributeKeys.CLOTHING_TYPE to setOf("bottom")), alphaSizes + waistSizes),
        group(mapOf(ListingAttributeKeys.CLOTHING_TYPE to setOf("accessory")), accessorySizes),
        group(mapOf(ListingAttributeKeys.CLOTHING_TYPE to setOf("other")), alphaSizes + accessorySizes),
        group(shoeRequirements("us_men"), usShoeSizes),
        group(shoeRequirements("us_women"), usShoeSizes),
        group(shoeRequirements("eu"), euShoeSizes)
    )

    private val allSizes = sizeGroups.flatMap { it.options }.distinctBy { it.code }

    val definitions = listOf(
        ListingAttributeDefinition(
            ListingAttributeKeys.CLOTHING_TYPE,
            "Clothing type",
            ListingAttributeType.Choice(clothingTypes)
        ),
        ListingAttributeDefinition(
            ListingAttributeKeys.SHOE_SIZE_SYSTEM,
            "Shoe size system",
            ListingAttributeType.Choice(shoeSizeSystems),
            ListingAttributeApplicability(ListingAttributeKeys.CLOTHING_TYPE, setOf("shoes"))
        ),
        ListingAttributeDefinition(
            ListingAttributeKeys.SIZE,
            "Size",
            ListingAttributeType.Choice(allSizes),
            ListingAttributeApplicability(
                ListingAttributeKeys.CLOTHING_TYPE,
                clothingTypes.map { it.code }.toSet()
            ),
            optionGroups = sizeGroups
        ),
        ListingAttributeDefinition(
            ListingAttributeKeys.COLOR,
            "Color",
            ListingAttributeType.Choice(colorOptions)
        ),
        ListingAttributeDefinition(
            ListingAttributeKeys.BRAND,
            "Brand",
            ListingAttributeType.Text(ListingTextFormat.BRAND, BRAND_MAX_LENGTH)
        )
    )

    private fun shoeRequirements(system: String) = mapOf(
        ListingAttributeKeys.CLOTHING_TYPE to setOf("shoes"),
        ListingAttributeKeys.SHOE_SIZE_SYSTEM to setOf(system)
    )

    private fun group(
        requiredChoices: Map<String, Set<String>>,
        options: List<ListingAttributeOption>
    ) = ListingAttributeOptionGroup(requiredChoices, options)

    private fun halfSizes(min: Int, max: Int): List<ListingAttributeOption> =
        (min * 2..max * 2).map { half ->
            val code = if (half % 2 == 0) (half / 2).toString() else "${half / 2}.5"
            ListingAttributeOption(code, code)
        }

    private fun options(vararg codes: String): List<ListingAttributeOption> =
        codes.map { code -> ListingAttributeOption(code, code) }

    private fun options(vararg pairs: Pair<String, String>): List<ListingAttributeOption> =
        pairs.map { (code, label) -> ListingAttributeOption(code, label) }
}
