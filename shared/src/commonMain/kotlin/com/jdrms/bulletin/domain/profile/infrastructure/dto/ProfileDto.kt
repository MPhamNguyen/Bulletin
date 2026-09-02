package com.jdrms.bulletin.domain.profile.infrastructure.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String,
    @SerialName("full_name") val fullName: String,
    @SerialName("major") val major: String = "",
    @SerialName("university") val university: String,
    @SerialName("bio") val bio: String = "",
    @SerialName("is_verified") val isVerified: Boolean = false
)

@Serializable
data class ReviewDto(
    @SerialName("id") val id: String = "",
    @SerialName("reviewer_id") val reviewerId: String,
    @SerialName("reviewer_name") val reviewerName: String = "Student",
    @SerialName("reviewee_id") val revieweeId: String,
    @SerialName("score") val score: Int,
    @SerialName("comment") val comment: String,
    @SerialName("created_at_millis") val createdAtMillis: Long = 0L
)

@Serializable
data class ReviewInsertDto(
    @SerialName("reviewer_id") val reviewerId: String,
    @SerialName("reviewee_id") val revieweeId: String,
    @SerialName("score") val score: Int,
    @SerialName("comment") val comment: String,
    @SerialName("created_at_millis") val createdAtMillis: Long = 0L
)
