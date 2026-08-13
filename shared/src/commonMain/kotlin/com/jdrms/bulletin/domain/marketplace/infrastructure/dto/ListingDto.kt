package com.jdrms.bulletin.domain.marketplace.infrastructure.dto

data class ListingDto(
    val id: String,
    val sellerId: String,
    val sellerName: String,
    val title: String,
    val description: String,
    val priceAmount: Double,
    val priceCurrency: String = "USD",
    val categoryName: String,
    val statusCode: String = "AVAILABLE",
    val imageUrl: String? = null,
    val createdAtMillis: Long = 0L
)
