package com.jdrms.bulletin.domain.listings.domain.model

data class ListingImageUpload(
    val bytes: ByteArray,
    val mediaType: String,
    val cropScale: Float = 1f,
    val cropOffsetX: Float = 0f,
    val cropOffsetY: Float = 0f
) {
    init {
        require(bytes.isNotEmpty()) { "An image cannot be empty." }
        require(bytes.size <= MAX_LISTING_IMAGE_SIZE_BYTES) { "Listing images must be 5 MB or smaller." }
        require(mediaType in SUPPORTED_LISTING_IMAGE_TYPES) { "Choose a JPEG, PNG, or WebP image." }
        require(cropScale in 1f..4f && cropOffsetX in -1f..1f && cropOffsetY in -1f..1f) {
            "Listing image framing is invalid."
        }
    }

    override fun equals(other: Any?): Boolean =
        other is ListingImageUpload && bytes.contentEquals(other.bytes) && mediaType == other.mediaType &&
            cropScale == other.cropScale && cropOffsetX == other.cropOffsetX && cropOffsetY == other.cropOffsetY

    override fun hashCode(): Int = listOf(bytes.contentHashCode(), mediaType, cropScale, cropOffsetX, cropOffsetY).hashCode()

    companion object {
        const val MAX_LISTING_IMAGE_COUNT = 5
        const val MAX_LISTING_IMAGE_SIZE_BYTES = 5 * 1024 * 1024
        val SUPPORTED_LISTING_IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/webp")
    }
}
