package com.jdrms.bulletin.domain.profile.infrastructure.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class PublicProfilePhotoDto(
    @SerialName("avatar_url") val avatarUrl: String?
)
