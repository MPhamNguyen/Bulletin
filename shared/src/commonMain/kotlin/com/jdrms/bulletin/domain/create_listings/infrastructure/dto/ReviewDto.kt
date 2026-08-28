package com.jdrms.bulletin.domain.create_listings.infrastructure.dto

data class ReviewDto(
    val id: String,
    val reviewerId: String,
    val reviewerName: String,
    val revieweeId: String,
    val score: Int,
    val comment: String,
    val createdAtMillis: Long
)
