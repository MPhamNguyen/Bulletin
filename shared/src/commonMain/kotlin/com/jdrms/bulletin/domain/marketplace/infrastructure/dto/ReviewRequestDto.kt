package com.jdrms.bulletin.domain.marketplace.infrastructure.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReviewRequestDto(
    @SerialName("listing_id") val listingId: String,
    @SerialName("buyer_id") val buyerId: String,
    @SerialName("seller_id") val sellerId: String,
    @SerialName("listing_title") val listingTitle: String,
    @SerialName("status") val status: String
)
