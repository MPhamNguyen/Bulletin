package com.jdrms.bulletin.domain.marketplace.infrastructure.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MarketplaceListingOwnerDto(
    @SerialName("user_id") val userId: String
)
