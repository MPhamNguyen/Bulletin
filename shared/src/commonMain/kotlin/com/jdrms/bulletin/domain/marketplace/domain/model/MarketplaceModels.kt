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
