package com.jdrms.bulletin.domain.marketplace.domain.service

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributeValue
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceAttributes

/** Rejects stale capacities and invalid numeric or calendar facts at the marketplace read boundary. */
internal object MarketplaceAttributeReadPolicy {
    private val ramDevices = setOf("laptop", "desktop", "tablet", "phone")

    fun recognized(attributes: MarketplaceAttributes): MarketplaceAttributes = MarketplaceAttributes(
        attributes.values.filter { (key, value) ->
            when (value) {
                is MarketplaceAttributeValue.Integer -> validInteger(key, value.value, attributes)
                is MarketplaceAttributeValue.Decimal -> validDecimal(key, value.plain)
                is MarketplaceAttributeValue.Date -> validDate(value.iso)
                is MarketplaceAttributeValue.Text -> value.value.none(Char::isISOControl)
                is MarketplaceAttributeValue.Choice -> true
            }
        }
    )

    private fun validInteger(key: String, number: Long, attributes: MarketplaceAttributes): Boolean {
        val device = (attributes["device_type"] as? MarketplaceAttributeValue.Choice)?.code
        return when (key) {
            "ram_gb" -> number in 1..4096 && device in ramDevices
            "storage_gb" -> number in 1..100_000 && (device in ramDevices || device == "console")
            "edition" -> number in 1..99
            else -> false
        }
    }

    private fun validDecimal(key: String, plain: String): Boolean {
        val fraction = plain.substringAfter('.', "").trimEnd('0')
        val number = plain.toDoubleOrNull() ?: return false
        return when (key) {
            "width_cm" -> fraction.length <= 1 && number in 0.1..1000.0
            "monthly_rent_usd" -> fraction.length <= 2 && number in 0.01..99999.99
            else -> false
        }
    }

    private fun validDate(iso: String): Boolean {
        val parts = iso.split('-').map { it.toIntOrNull() }
        val year = parts.getOrNull(0) ?: return false
        val month = parts.getOrNull(1) ?: return false
        val day = parts.getOrNull(2) ?: return false
        val leap = year % 4 == 0 && (year % 100 != 0 || year % 400 == 0)
        val days = when (month) {
            2 -> if (leap) 29 else 28
            4, 6, 9, 11 -> 30
            else -> 31
        }
        return year in 2000..2100 && month in 1..12 && day in 1..days
    }
}
