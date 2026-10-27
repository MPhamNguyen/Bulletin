package com.jdrms.bulletin.domain.marketplace.domain.model

import com.jdrms.bulletin.core.common.formatUsdAmount
import com.jdrms.bulletin.core.common.hasAtMostTwoDecimalPlaces
import kotlin.jvm.JvmInline

@JvmInline
value class MarketplaceItemId(val value: String)

@JvmInline
value class MarketplaceSellerId(val value: String)

enum class MarketplaceCategory {
    TEXTBOOKS,
    ELECTRONICS,
    FURNITURE,
    CLOTHING,
    HOUSING,
    OTHER
}

enum class MarketplaceCondition {
    NEW,
    LIKE_NEW,
    GOOD,
    FAIR,
    POOR,
    UNKNOWN;

    companion object {
        val knownValues: List<MarketplaceCondition>
            get() = entries.filterNot { it == UNKNOWN }
    }
}

data class MarketplacePrice(
    val amount: Double,
    val currency: String = "USD"
) {
    init {
        require(amount >= 0.0) { "Marketplace price amount cannot be negative." }
        require(hasAtMostTwoDecimalPlaces(amount)) {
            "Marketplace price amount cannot have more than two decimal places."
        }
    }

    val formatted: String
        get() = formatUsdAmount(amount)
}

data class MarketplaceItem(
    val id: MarketplaceItemId,
    val sellerId: String,
    val sellerName: String,
    val title: String,
    val description: String,
    val price: MarketplacePrice,
    val category: MarketplaceCategory,
    val condition: MarketplaceCondition = MarketplaceCondition.UNKNOWN,
    val isBookmarked: Boolean = false,
    val createdAtMillis: Long = 0L,
    val photos: List<String> = emptyList(),
    val updatedAtMillis: Long = 0L
) {
    init {
        require(title.isNotBlank()) { "Item title cannot be blank." }
        require(sellerName.isNotBlank()) { "Seller name cannot be blank." }
    }
}

typealias MarketplaceListing = Listing

data class Listing(
    val id: MarketplaceItemId,
    val sellerId: String,
    val buyerId: String? = null,
    val sellerName: String,
    val title: String,
    val description: String,
    val price: MarketplacePrice,
    val category: MarketplaceCategory,
    val condition: String = "UNKNOWN",
    val status: String = "AVAILABLE",
    val isSold: Boolean = false,
    val photos: List<String> = emptyList(),
    val sellerReputationScore: Double? = null,
    val isBookmarked: Boolean = false,
    val createdAtMillis: Long = 0L,
    val sellerSchool: String = "CSU Long Beach",
    val sellerAvatarUrl: String? = null,
    val sellerMajor: String = "",
    val sellerReviewCount: Int = 0,
    val sellerIsVerified: Boolean = false,
    val updatedAtMillis: Long = 0L
) {
    init {
        require(title.isNotBlank()) { "Listing title cannot be blank." }
        require(title.trim().length >= 3) { "Listing title must be at least 3 characters." }
        require(description.isNotBlank()) { "Listing description cannot be blank." }
        require(sellerName.isNotBlank()) { "Seller name cannot be blank." }
        require(sellerSchool.isNotBlank()) { "Seller school cannot be blank." }
        require(sellerReviewCount >= 0) { "Seller review count cannot be negative." }
        sellerReputationScore?.let {
            require(it in 0.0..5.0) { "Reputation score must be between 0.0 and 5.0." }
        }
        photos.forEach { photoUrl ->
            require(photoUrl.isNotBlank()) { "Photo URL cannot be blank." }
        }
    }
}

@JvmInline
value class MarketplaceReviewId(val value: String)

data class MarketplaceReview(
    val id: MarketplaceReviewId = MarketplaceReviewId(""),
    val listingId: MarketplaceItemId,
    val reviewerId: String,
    val revieweeId: String,
    val score: Int,
    val comment: String
) {
    init {
        require(listingId.value.isNotBlank()) { "Review listing ID cannot be blank." }
        require(reviewerId.isNotBlank()) { "Review reviewer ID cannot be blank." }
        require(revieweeId.isNotBlank()) { "Review reviewee ID cannot be blank." }
        require(score in 1..5) { "Review score must be between 1 and 5." }
    }
}
