package com.jdrms.bulletin.domain.listings.domain.model

data class ListingImageUpload(
    val bytes: ByteArray,
    val mediaType: String
) {
    init {
        require(bytes.isNotEmpty()) { "An image cannot be empty." }
        require(bytes.size <= MAX_LISTING_IMAGE_SIZE_BYTES) { "Listing images must be 5 MB or smaller." }
        require(mediaType in SUPPORTED_LISTING_IMAGE_TYPES) { "Choose a JPEG, PNG, or WebP image." }
    }

    override fun equals(other: Any?): Boolean =
        other is ListingImageUpload && bytes.contentEquals(other.bytes) && mediaType == other.mediaType

    override fun hashCode(): Int = 31 * bytes.contentHashCode() + mediaType.hashCode()

    companion object {
        const val MAX_LISTING_IMAGE_COUNT = 5
        const val MAX_LISTING_IMAGE_SIZE_BYTES = 5 * 1024 * 1024
        val SUPPORTED_LISTING_IMAGE_TYPES = setOf("image/jpeg", "image/png", "image/webp")
    }
}
