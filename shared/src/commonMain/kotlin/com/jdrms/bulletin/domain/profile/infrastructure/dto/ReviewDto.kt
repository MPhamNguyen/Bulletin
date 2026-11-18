package com.jdrms.bulletin.domain.profile.infrastructure.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReviewDto(
    @SerialName("id") val id: String = "",
    @SerialName("reviewer_id") val reviewerId: String?,
    @SerialName("reviewer_name") val reviewerName: String? = null,
    @SerialName("reviewee_id") val revieweeId: String,
    @SerialName("score") val score: Int,
    @SerialName("comment") val comment: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("listing_id") val listingId: String? = null,
    @SerialName("was_listing_review") val wasListingReview: Boolean = false
)
