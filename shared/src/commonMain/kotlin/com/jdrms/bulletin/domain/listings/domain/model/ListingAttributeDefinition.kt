package com.jdrms.bulletin.domain.listings.domain.model

/** How a free-text attribute is normalized for storage and compared when matching. */
enum class ListingTextFormat {
    /** Display spelling is kept; matching uses a separate normalized key. */
    BRAND,
    ISBN,
    COURSE_CODE,
    CLOTHING_SIZE
}

enum class ListingAttributeUnit(val prefix: String, val suffix: String) {
    GIGABYTES("", " GB"),
    CENTIMETERS("", " cm"),
    USD_PER_MONTH("$", "/mo")
}

data class ListingAttributeOption(val code: String, val label: String)

sealed interface ListingAttributeType {
    data class Text(val format: ListingTextFormat, val maxLength: Int) : ListingAttributeType
    data class Choice(val options: List<ListingAttributeOption>) : ListingAttributeType
    data class Integer(val min: Long, val max: Long, val unit: ListingAttributeUnit? = null) : ListingAttributeType
    data class Decimal(
        val min: ListingDecimal,
        val max: ListingDecimal,
        val unit: ListingAttributeUnit? = null
    ) : ListingAttributeType {
        val scale: Int get() = min.scale
    }

    data class Date(val minYear: Int, val maxYear: Int) : ListingAttributeType
}

/** The attribute is only meaningful while a sibling choice attribute holds one of [allowedCodes]. */
data class ListingAttributeApplicability(val controllingKey: String, val allowedCodes: Set<String>)

data class ListingAttributeDefinition(
    val key: String,
    val label: String,
    val type: ListingAttributeType,
    val applicability: ListingAttributeApplicability? = null
) {
    /** Another category's definition keeps a value only when key and type are identical. */
    fun isCompatibleWith(other: ListingAttributeDefinition): Boolean = key == other.key && type == other.type
}
