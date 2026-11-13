package com.jdrms.bulletin.domain.profile.infrastructure.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReputationSummaryDto(
    @SerialName("average_rating") val averageRating: Double,
    @SerialName("total_reviews") val totalReviews: Long
)
