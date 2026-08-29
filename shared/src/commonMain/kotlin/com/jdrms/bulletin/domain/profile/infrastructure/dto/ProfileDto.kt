package com.jdrms.bulletin.domain.profile.infrastructure.dto

data class ProfileDto(
    val id: String,
    val email: String,
    val fullName: String,
    val university: String,
    val bio: String = "",
    val isVerified: Boolean = false
)

data class ReviewDto(
    val id: String,
    val reviewerId: String,
    val reviewerName: String,
    val revieweeId: String,
    val score: Int,
    val comment: String,
    val createdAtMillis: Long = 0L
)
