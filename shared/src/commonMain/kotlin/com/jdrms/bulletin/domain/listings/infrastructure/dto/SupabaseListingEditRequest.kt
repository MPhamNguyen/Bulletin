package com.jdrms.bulletin.domain.listings.infrastructure.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SupabaseListingEditRequest(
    @SerialName("p_listing") val listing: SupabaseListingWriteDto
)
