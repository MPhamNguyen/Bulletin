package com.jdrms.bulletin.domain.home.domain.model

import com.jdrms.bulletin.domain.marketplace.domain.model.Category
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import kotlin.jvm.JvmInline

@JvmInline
value class RecommendationId(val value: String)

data class UserPreferences(
    val preferredCategories: List<Category> = listOf(Category.TEXTBOOKS, Category.ELECTRONICS),
    val maxPrice: Double? = 500.0,
    val campusLocation: String = "CSU Long Beach"
)

data class Recommendation(
    val id: RecommendationId,
    val listing: Listing,
    val score: Double,
    val reason: String
)
