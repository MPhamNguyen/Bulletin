package com.jdrms.bulletin.domain.listings.domain.model

import com.jdrms.bulletin.core.common.formatUsdAmount
import com.jdrms.bulletin.core.common.hasAtMostTwoDecimalPlaces
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
        require(hasAtMostTwoDecimalPlaces(amount)) {
            "Listing price cannot have more than two decimal places."
        }
    }

    val formatted: String
        get() = formatUsdAmount(amount)
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
    val images: List<String> = emptyList(),
    val createdAtMillis: Long = 0L
) {
    init {
        require(title.isNotBlank()) { "Listing title cannot be blank." }
        require(title.length >= 3) { "Listing title must be at least 3 characters." }
        require(description.isNotBlank()) { "Listing description cannot be blank." }
        require(sellerName.isNotBlank()) { "Seller name cannot be blank." }
    }

    fun isOwnedBy(sellerId: SellerId): Boolean = this.sellerId == sellerId

    fun updateDetails(
        editorSellerId: SellerId,
        title: String = this.title,
        description: String = this.description,
        price: ListingPrice = this.price,
        category: ListingCategory = this.category,
        condition: ListingCondition = this.condition,
        images: List<String> = this.images
    ): Listing {
        require(isOwnedBy(editorSellerId)) { "Only the owner can edit this listing." }
        return copy(
            title = title,
            description = description,
            price = price,
            category = category,
            condition = condition,
            images = images
        )
    }
}
