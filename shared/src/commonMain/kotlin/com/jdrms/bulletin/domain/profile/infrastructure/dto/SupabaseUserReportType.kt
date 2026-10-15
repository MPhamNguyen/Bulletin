package com.jdrms.bulletin.domain.profile.infrastructure.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SupabaseUserReportType {
    @SerialName("user")
    USER
}
