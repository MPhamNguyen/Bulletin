package com.jdrms.bulletin.domain.home.infrastructure.dto

data class HomeFeedItemDto(
    val id: String,
    val title: String,
    val description: String,
    val price: Double,
    val category: String,
    val score: Double = 0.0,
    val reason: String = "",
    val imageUrl: String? = null
)
