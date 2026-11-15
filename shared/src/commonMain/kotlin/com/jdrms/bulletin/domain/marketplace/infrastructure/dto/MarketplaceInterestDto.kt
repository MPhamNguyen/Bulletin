package com.jdrms.bulletin.domain.marketplace.infrastructure.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class MarketplaceInterestDto(
    @SerialName("category") val category: String,
    @SerialName("condition") val condition: String? = null,
    @SerialName("minimum_price_cents") val minimumPriceCents: Long? = null,
    @SerialName("maximum_price_cents") val maximumPriceCents: Long? = null,
    @SerialName("attribute_filters") val attributeFilters: JsonObject = JsonObject(emptyMap())
)
