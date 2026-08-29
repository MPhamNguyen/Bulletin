package com.jdrms.bulletin.domain.marketplace.infrastructure.dto

data class MarketplaceItemDto(
    val id: String,
    val sellerId: String,
    val sellerName: String,
    val title: String,
    val description: String,
    val price: Double,
    val category: String,
    val createdAtMillis: Long = 0L
)
