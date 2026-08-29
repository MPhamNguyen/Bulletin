package com.jdrms.bulletin.domain.listings.infrastructure.dto

data class ListingDto(
    val id: String,
    val sellerId: String,
    val sellerName: String,
    val title: String,
    val description: String,
    val price: Double,
    val category: String,
    val condition: String = "GOOD",
    val status: String = "AVAILABLE",
    val createdAtMillis: Long = 0L
)
