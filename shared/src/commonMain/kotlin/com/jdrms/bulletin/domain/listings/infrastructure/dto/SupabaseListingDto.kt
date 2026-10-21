package com.jdrms.bulletin.domain.listings.infrastructure.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SupabaseListingDto(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("category") val category: String? = null,
    @SerialName("condition") val condition: String? = null,
    @SerialName("price") val price: Double? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("status") val status: String? = null,
    @SerialName("is_sold") val isSold: Boolean = false,
    @SerialName("seller_profile") val profile: SupabaseListingProfileDto? = null,
    @SerialName("buyer_id") val buyerId: String? = null,
    @SerialName("listing_images") val listingImages: List<SupabaseListingImageDto> = emptyList()
)

@Serializable
data class SupabaseListingImageDto(
    @SerialName("object_path") val objectPath: String,
    @SerialName("position") val position: Int,
    @SerialName("crop_scale") val cropScale: Float = 1f,
    @SerialName("crop_offset_x") val cropOffsetX: Float = 0f,
    @SerialName("crop_offset_y") val cropOffsetY: Float = 0f
)

@Serializable
data class SupabaseListingProfileDto(
    @SerialName("full_name") val fullName: String? = null
)

typealias SupabaseListingInsertDto = SupabaseListingWriteDto
