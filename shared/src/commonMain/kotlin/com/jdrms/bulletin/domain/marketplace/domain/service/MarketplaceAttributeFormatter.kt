package com.jdrms.bulletin.domain.marketplace.domain.service

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeCatalog
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeSpec
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeValue
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributes
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory

data class MarketplaceAttributeLine(val label: String, val value: String)

/** Readable "label: value" lines for the item-details section. Empty attributes produce no lines. */
object MarketplaceAttributeFormatter {
    private val MONTHS = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
    private const val GROUP_SIZE = 3

    fun linesFor(category: MarketplaceCategory, attributes: MarketplaceAttributes): List<MarketplaceAttributeLine> =
        MarketplaceAttributeCatalog.specsFor(category).mapNotNull { spec ->
            attributes[spec.key]?.let { MarketplaceAttributeLine(spec.label, format(spec, it)) }
        }

    fun format(spec: MarketplaceAttributeSpec, value: MarketplaceAttributeValue): String = when (value) {
        is MarketplaceAttributeValue.Text -> value.value
        is MarketplaceAttributeValue.Choice -> spec.choices[value.code] ?: value.code
        is MarketplaceAttributeValue.Integer -> spec.prefix + value.value + spec.suffix
        is MarketplaceAttributeValue.Decimal -> spec.prefix + group(value.plain, spec.prefix.isNotEmpty()) + spec.suffix
        is MarketplaceAttributeValue.Date -> formatDate(value.iso)
    }

    private fun group(plain: String, money: Boolean): String {
        if (!money) return plain
        val whole = plain.substringBefore('.')
        return whole.reversed().chunked(GROUP_SIZE).joinToString(",").reversed() + plain.substring(whole.length)
    }

    private fun formatDate(iso: String): String {
        val parts = iso.split('-')
        val month = parts.getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..MONTHS.size } ?: return iso
        val day = parts.getOrNull(2)?.toIntOrNull() ?: return iso
        return "${MONTHS[month - 1]} $day, ${parts[0]}"
    }
}
