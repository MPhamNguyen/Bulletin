package com.jdrms.bulletin.domain.profile.infrastructure.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ReviewScoreDto(
    @SerialName("score") val score: Int
)
