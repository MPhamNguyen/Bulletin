package com.jdrms.bulletin.domain.marketplace.domain.model

/** Typed, read-only attribute value as buyers see it. Decimals and dates keep their exact plain text. */
sealed interface MarketplaceAttributeValue {
    data class Text(val value: String) : MarketplaceAttributeValue
    data class Choice(val code: String) : MarketplaceAttributeValue
    data class Integer(val value: Long) : MarketplaceAttributeValue
    data class Decimal(val plain: String) : MarketplaceAttributeValue
    data class Date(val iso: String) : MarketplaceAttributeValue
}

/** Recognized item facts for one listing. Entries marketplace cannot interpret are never carried. */
data class MarketplaceAttributes(val values: Map<String, MarketplaceAttributeValue> = emptyMap()) {
    fun isEmpty(): Boolean = values.isEmpty()

    operator fun get(key: String): MarketplaceAttributeValue? = values[key]

    companion object {
        val EMPTY = MarketplaceAttributes()
    }
}
