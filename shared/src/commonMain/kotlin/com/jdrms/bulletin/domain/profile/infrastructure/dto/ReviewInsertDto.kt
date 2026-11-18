package com.jdrms.bulletin.domain.profile.infrastructure.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReviewInsertDto(
    @SerialName("reviewer_id") val reviewerId: String,
    @SerialName("reviewee_id") val revieweeId: String,
    @SerialName("score") val score: Int,
    @SerialName("comment") val comment: String,
    @SerialName("listing_id") val listingId: String? = null
)
