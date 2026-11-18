package com.jdrms.bulletin.domain.profile.infrastructure.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ProfileDto(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("major") val major: String? = null,
    @SerialName("university") val university: String? = null,
    @SerialName("bio") val bio: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("is_verified") val isVerified: Boolean? = null,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class ProfileUpdateDto(
    @SerialName("full_name") val fullName: String,
    @SerialName("major") val major: String = "",
    @SerialName("university") val university: String,
    @SerialName("bio") val bio: String = "",
    // No default: null must be sent to clear an existing photo.
    @SerialName("avatar_url") val avatarUrl: String?
)
