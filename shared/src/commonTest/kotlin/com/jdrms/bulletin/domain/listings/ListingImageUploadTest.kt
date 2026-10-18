package com.jdrms.bulletin.domain.listings

import com.jdrms.bulletin.domain.listings.domain.model.ListingImageUpload
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ListingImageUploadTest {
    @Test
    fun acceptsSupportedImageTypesAtTheSizeLimit() {
        ListingImageUpload(ByteArray(ListingImageUpload.MAX_LISTING_IMAGE_SIZE_BYTES), "image/webp")
    }

    @Test
    fun rejectsEmptyOversizedAndUnsupportedImages() {
        assertEquals(
            "An image cannot be empty.",
            assertFailsWith<IllegalArgumentException> {
                ListingImageUpload(byteArrayOf(), "image/jpeg")
            }.message
        )
        assertEquals(
            "Listing images must be 5 MB or smaller.",
            assertFailsWith<IllegalArgumentException> {
                ListingImageUpload(ByteArray(ListingImageUpload.MAX_LISTING_IMAGE_SIZE_BYTES + 1), "image/png")
            }.message
        )
        assertEquals(
            "Choose a JPEG, PNG, or WebP image.",
            assertFailsWith<IllegalArgumentException> {
                ListingImageUpload(byteArrayOf(1), "image/gif")
            }.message
        )
    }
}
