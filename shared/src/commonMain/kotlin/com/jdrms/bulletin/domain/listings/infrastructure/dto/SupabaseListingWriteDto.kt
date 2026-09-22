package com.jdrms.bulletin.domain.listings.infrastructure.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseListingWriteDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("user_id") val userId: String,
    @SerialName("category") val category: String,
    @SerialName("condition") val condition: String,
    @SerialName("price") val price: Double,
    @SerialName("description") val description: String
)
