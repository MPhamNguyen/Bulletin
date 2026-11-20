package com.jdrms.bulletin.domain.listings.infrastructure.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class SupabaseListingWriteDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("user_id") val userId: String,
    @SerialName("category") val category: String,
    @SerialName("condition") val condition: String,
    @SerialName("price") val price: Double,
    @SerialName("description") val description: String,
    @SerialName("status") val status: String,
    /** Required even when null: restore must clear the persisted buyer rather than omit the column. */
    @SerialName("buyer_id") val buyerId: String?,
    /** Deliberately has no default: it must always be encoded so clearing every attribute persists as `{}`. */
    @SerialName("attributes") val attributes: JsonObject
)
