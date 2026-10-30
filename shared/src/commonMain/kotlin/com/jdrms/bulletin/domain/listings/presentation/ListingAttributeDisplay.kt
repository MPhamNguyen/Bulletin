package com.jdrms.bulletin.domain.listings.presentation

import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeCatalog
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDate
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeDefinition
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeType
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeUnit
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributeValue
import com.jdrms.bulletin.domain.listings.domain.model.ListingAttributes
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingDecimal

data class ListingAttributeLine(val label: String, val value: String)

/**
 * Display text for attributes. Only recognized, valid, nonempty values are shown; preserved entries (unknown,
 * retired, invalid) are deliberately hidden because this client cannot describe them accurately.
 */
object ListingAttributeDisplay {
    private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    fun linesFor(category: ListingCategory, attributes: ListingAttributes): List<ListingAttributeLine> =
        ListingAttributeCatalog.definitionsFor(category).mapNotNull { definition ->
            attributes[definition.key]?.let { value ->
                ListingAttributeLine(
                    definition.label,
                    format(definition, value)
                )
            }
        }

    /** Form label: the attribute name with its unit, for example `RAM (GB)` or `Monthly rent (USD per month)`. */
    fun fieldLabel(definition: ListingAttributeDefinition): String {
        val unit = when (
            (definition.type as? ListingAttributeType.Integer)?.unit
                ?: (definition.type as? ListingAttributeType.Decimal)?.unit
        ) {
            ListingAttributeUnit.GIGABYTES -> " (GB)"
            ListingAttributeUnit.CENTIMETERS -> " (cm)"
            ListingAttributeUnit.USD_PER_MONTH -> " (USD per month)"
            null -> ""
        }
        return definition.label + unit
    }

    fun format(definition: ListingAttributeDefinition, value: ListingAttributeValue): String = when (value) {
        is ListingAttributeValue.Text -> value.value
        is ListingAttributeValue.Choice ->
            (definition.type as? ListingAttributeType.Choice)?.options?.firstOrNull { it.code == value.code }?.label
                ?: value.code
        is ListingAttributeValue.Integer -> withUnit(value.value.toString(), unitOf(definition))
        is ListingAttributeValue.Decimal -> withUnit(formatDecimal(value.value, unitOf(definition)), unitOf(definition))
        is ListingAttributeValue.Date -> formatDate(value.value)
    }

    private fun unitOf(definition: ListingAttributeDefinition): ListingAttributeUnit? =
        (definition.type as? ListingAttributeType.Integer)?.unit
            ?: (definition.type as? ListingAttributeType.Decimal)?.unit

    private fun withUnit(text: String, unit: ListingAttributeUnit?): String =
        if (unit == null) text else unit.prefix + text + unit.suffix

    private fun formatDecimal(value: ListingDecimal, unit: ListingAttributeUnit?): String {
        val plain = value.toPlainString()
        if (unit != ListingAttributeUnit.USD_PER_MONTH) return plain
        val whole = plain.substringBefore('.')
        val grouped = whole.reversed().chunked(GROUP_SIZE).joinToString(",").reversed()
        return grouped + plain.substring(whole.length)
    }

    private fun formatDate(date: ListingAttributeDate): String = "${MONTHS[date.month - 1]} ${date.day}, ${date.year}"

    private const val GROUP_SIZE = 3
}
