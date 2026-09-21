package com.jdrms.bulletin.domain.marketplace.infrastructure.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseMarketplaceListingDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("condition") val condition: String? = null,
    @SerialName("price") val price: Double? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)
