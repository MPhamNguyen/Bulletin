package com.jdrms.bulletin.domain.listings.domain.service

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeValue
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributes

/** Inverse of [ListingAttributeInputParser]: typed values back to the text a form field edits. */
object ListingAttributeInputFormatter {
    fun inputsFor(attributes: ListingAttributes): Map<String, String> =
        attributes.values.mapValues { (_, value) -> inputFor(value) }

    fun inputFor(value: ListingAttributeValue): String = when (value) {
        is ListingAttributeValue.Text -> value.value
        is ListingAttributeValue.Choice -> value.code
        is ListingAttributeValue.Integer -> value.value.toString()
        is ListingAttributeValue.Decimal -> value.value.toPlainString()
        is ListingAttributeValue.Date -> value.value.toString()
    }
}
