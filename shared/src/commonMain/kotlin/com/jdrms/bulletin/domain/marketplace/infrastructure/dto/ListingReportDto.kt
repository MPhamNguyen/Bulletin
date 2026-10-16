package com.jdrms.bulletin.domain.marketplace.infrastructure.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SupabaseListingReportType {
    @SerialName("listing")
    LISTING
}

@Serializable
data class ListingReportInsertDto(
    @SerialName("reporter_id") val reporterId: String,
    @SerialName("type") val type: SupabaseListingReportType,
    @SerialName("listing_id") val listingId: String,
    @SerialName("reason") val reason: String,
    @SerialName("description") val description: String?
)

@Serializable
data class ListingReportListingIdDto(
    @SerialName("listing_id") val listingId: String
)
