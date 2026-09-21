package com.jdrms.bulletin.domain.marketplace.infrastructure.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MarketplaceItemDto(
    @SerialName("id") val id: String,
    @SerialName("seller_id") val sellerId: String,
    @SerialName("seller_name") val sellerName: String = "",
    @SerialName("title") val title: String,
    @SerialName("description") val description: String = "",
    @SerialName("price") val price: Double,
    @SerialName("category") val category: String = "OTHER",
    @SerialName("created_at_millis") val createdAtMillis: Long = 0L
)

@Serializable
data class MarketplaceListingDto(
    @SerialName("id") val id: String,
    @SerialName("seller_id") val sellerId: String,
    @SerialName("seller_name") val sellerName: String = "",
    @SerialName("title") val title: String,
    @SerialName("description") val description: String = "",
    @SerialName("price") val price: Double,
    @SerialName("category") val category: String = "OTHER",
    @SerialName("condition") val condition: String = "GOOD",
    @SerialName("status") val status: String = "AVAILABLE",
    @SerialName("photos") val photos: List<String> = emptyList(),
    @SerialName("reputation_score") val reputationScore: Double? = null,
    @SerialName("created_at_millis") val createdAtMillis: Long = 0L
)

@Serializable
data class ReviewScoreDto(
    @SerialName("score") val score: Int
)
