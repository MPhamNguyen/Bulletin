package com.jdrms.bulletin.domain.messages.infrastructure.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class SupabaseMessageReportType {
    @SerialName("message")
    MESSAGE
}
