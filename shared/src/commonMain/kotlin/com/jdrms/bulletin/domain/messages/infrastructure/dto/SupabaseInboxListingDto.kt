package com.jdrms.bulletin.domain.messages.infrastructure.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Messages-owned projection of the linked listing's cover image; no listing aggregate is loaded. */
@Serializable
data class SupabaseInboxListingDto(
    @SerialName("listing_images") val images: List<SupabaseInboxListingImageDto> = emptyList()
)

@Serializable
data class SupabaseInboxListingImageDto(
    @SerialName("object_path") val objectPath: String,
    @SerialName("position") val position: Int,
    @SerialName("crop_scale") val cropScale: Float = 1f,
    @SerialName("crop_offset_x") val cropOffsetX: Float = 0f,
    @SerialName("crop_offset_y") val cropOffsetY: Float = 0f
)
