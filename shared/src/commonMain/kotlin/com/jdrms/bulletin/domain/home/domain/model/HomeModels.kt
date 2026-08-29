package com.jdrms.bulletin.domain.home.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class HomeItemId(val value: String)

enum class HomeFeedCategory {
    TEXTBOOKS,
    ELECTRONICS,
    FURNITURE,
    CLOTHING,
    HOUSING,
    OTHER
}

data class HomePrice(
    val amount: Double,
    val currency: String = "USD"
) {
    init {
        require(amount >= 0.0) { "Price amount cannot be negative." }
    }

    val formatted: String
        get() = "$$amount"
}

data class UserPreferences(
    val preferredCategories: List<HomeFeedCategory> = listOf(
        HomeFeedCategory.TEXTBOOKS,
        HomeFeedCategory.ELECTRONICS
    ),
    val maxPrice: Double? = 500.0,
    val campusLocation: String = "CSU Long Beach"
)

data class HomeFeedItem(
    val id: HomeItemId,
    val title: String,
    val description: String,
    val price: HomePrice,
    val category: HomeFeedCategory,
    val score: Double = 0.0,
    val reason: String = "",
    val imageUrl: String? = null
) {
    init {
        require(title.isNotBlank()) { "Feed item title cannot be blank." }
        require(score >= 0.0) { "Recommendation score cannot be negative." }
    }
}
