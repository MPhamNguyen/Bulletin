package com.jdrms.bulletin.domain.marketplace.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class MarketplaceItemId(val value: String)

enum class MarketplaceCategory {
    TEXTBOOKS,
    ELECTRONICS,
    FURNITURE,
    CLOTHING,
    HOUSING,
    OTHER
}

data class MarketplacePrice(
    val amount: Double,
    val currency: String = "USD"
) {
    init {
        require(amount >= 0.0) { "Marketplace price amount cannot be negative." }
    }

    val formatted: String
        get() = "$$amount"
}

data class MarketplaceItem(
    val id: MarketplaceItemId,
    val sellerId: String,
    val sellerName: String,
    val title: String,
    val description: String,
    val price: MarketplacePrice,
    val category: MarketplaceCategory,
    val isSaved: Boolean = false,
    val createdAtMillis: Long = 0L
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
    val sellerName: String,
    val title: String,
    val description: String,
    val price: MarketplacePrice,
    val category: MarketplaceCategory,
    val condition: String = "GOOD",
    val status: String = "AVAILABLE",
    val photos: List<String> = emptyList(),
    val sellerReputationScore: Double? = null,
    val isSaved: Boolean = false,
    val createdAtMillis: Long = 0L
) {
    init {
        require(title.isNotBlank()) { "Listing title cannot be blank." }
        require(title.trim().length >= 3) { "Listing title must be at least 3 characters." }
        require(description.isNotBlank()) { "Listing description cannot be blank." }
        require(sellerName.isNotBlank()) { "Seller name cannot be blank." }
        sellerReputationScore?.let {
            require(it in 0.0..5.0) { "Reputation score must be between 0.0 and 5.0." }
        }
        photos.forEach { photoUrl ->
            require(photoUrl.isNotBlank()) { "Photo URL cannot be blank." }
        }
    }
}
