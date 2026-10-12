package com.jdrms.bulletin.domain.marketplace.infrastructure.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ListingBookmarkDto(
    @SerialName("user_id") val userId: String,
    @SerialName("listing_id") val listingId: String
)

@Serializable
data class ListingBookmarkWithListingDto(
    @SerialName("listing_id") val listingId: String,
    @SerialName("listings") val listing: SupabaseMarketplaceListingDto? = null
)
