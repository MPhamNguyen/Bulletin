package com.jdrms.bulletin.domain.reputation.infrastructure.dto

data class ReviewDto(
    val id: String,
    val reviewerId: String,
    val reviewerName: String,
    val revieweeId: String,
    val score: Int,
    val comment: String,
    val createdAtMillis: Long
)
