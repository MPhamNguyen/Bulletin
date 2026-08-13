package com.jdrms.bulletin.domain.recommendations.infrastructure.dto

import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.ListingDto

data class RecommendationDto(
    val id: String,
    val listing: ListingDto,
    val score: Double,
    val reason: String
)

data class PreferencesDto(
    val preferredCategories: List<String>,
    val maxPrice: Double?,
    val campusLocation: String
)
