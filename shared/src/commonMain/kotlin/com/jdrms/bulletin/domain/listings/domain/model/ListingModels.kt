package com.jdrms.bulletin.domain.listings.domain.model

import kotlin.jvm.JvmInline

@JvmInline
value class ListingId(val value: String)

@JvmInline
value class SellerId(val value: String)

enum class ListingCategory {
    TEXTBOOKS,
    ELECTRONICS,
    FURNITURE,
    CLOTHING,
    HOUSING,
    OTHER
}

enum class ListingCondition {
    NEW,
    LIKE_NEW,
    GOOD,
    FAIR,
    POOR
}

enum class ListingStatus {
    AVAILABLE,
    PENDING,
    SOLD,
    ARCHIVED
}

data class ListingPrice(
    val amount: Double,
    val currency: String = "USD"
) {
    init {
        require(amount >= 0.0) { "Listing price cannot be negative." }
    }

    val formatted: String
        get() = "$$amount"
}

data class Listing(
    val id: ListingId,
    val sellerId: SellerId,
    val sellerName: String,
    val title: String,
    val description: String,
    val price: ListingPrice,
    val category: ListingCategory,
    val condition: ListingCondition = ListingCondition.GOOD,
    val status: ListingStatus = ListingStatus.AVAILABLE,
    val createdAtMillis: Long = 0L
) {
    init {
        require(title.isNotBlank()) { "Listing title cannot be blank." }
        require(title.length >= 3) { "Listing title must be at least 3 characters." }
        require(description.isNotBlank()) { "Listing description cannot be blank." }
        require(sellerName.isNotBlank()) { "Seller name cannot be blank." }
    }
}
