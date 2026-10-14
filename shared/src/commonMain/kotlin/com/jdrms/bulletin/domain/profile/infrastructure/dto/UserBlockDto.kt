package com.jdrms.bulletin.domain.profile.infrastructure.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserBlockInsertDto(
    @SerialName("blocker_id") val blockerId: String,
    @SerialName("blocked_user_id") val blockedUserId: String
)

@Serializable
data class UserBlockRowDto(
    @SerialName("blocker_id") val blockerId: String,
    @SerialName("blocked_user_id") val blockedUserId: String
)
