package com.jdrms.bulletin.domain.listings.domain.model

/** A single typed attribute value. Numbers stay numbers, choices hold stable codes, never display labels. */
sealed interface ListingAttributeValue {
    data class Text(val value: String) : ListingAttributeValue
    data class Choice(val code: String) : ListingAttributeValue
    data class Integer(val value: Long) : ListingAttributeValue
    data class Decimal(val value: ListingDecimal) : ListingAttributeValue
    data class Date(val value: ListingAttributeDate) : ListingAttributeValue
}
