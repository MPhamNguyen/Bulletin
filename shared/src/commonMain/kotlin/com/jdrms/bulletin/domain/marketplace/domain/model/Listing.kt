package com.jdrms.bulletin.domain.marketplace.domain.model

data class Price(
    val amount: Double,
    val currency: String = "USD"
) {
    init {
        require(amount >= 0.0) { "Price amount cannot be negative." }
    }

    val formatted: String
        get() = "$$amount"
}

enum class Category {
    TEXTBOOKS,
    ELECTRONICS,
    FURNITURE,
    CLOTHING,
    HOUSING,
    OTHER
}

enum class ListingStatus {
    AVAILABLE,
    PENDING,
    SOLD
}

data class Listing(
    val id: ListingId,
    val sellerId: SellerId,
    val sellerName: String,
    val title: String,
    val description: String,
    val price: Price,
    val category: Category,
    val status: ListingStatus = ListingStatus.AVAILABLE,
    val imageUrl: String? = null,
    val createdAtMillis: Long = 0L
)
