package com.jdrms.bulletin.domain.listings.presentation

import kotlin.test.Test
import kotlin.test.assertEquals

class ListingImagePreviewDecoderTest {
    @Test
    fun readsRotationFromExifOrientedJpeg() {
        val transform = jpegWithOrientation(ExifOrientation.ROTATE_90).exifPreviewTransform()

        assertEquals(ExifPreviewTransform(rotationDegrees = 90, isFlipped = false), transform)
    }

    @Test
    fun readsMirroringFromExifOrientedJpeg() {
        val transform = jpegWithOrientation(ExifOrientation.FLIP_HORIZONTAL).exifPreviewTransform()

        assertEquals(ExifPreviewTransform(rotationDegrees = 0, isFlipped = true), transform)
    }

    private fun jpegWithOrientation(orientation: Int): ByteArray = byteArrayOf(
        0xFF.toByte(), 0xD8.toByte(),
        0xFF.toByte(), 0xE1.toByte(), 0x00, 0x22,
        0x45, 0x78, 0x69, 0x66, 0x00, 0x00,
        0x49, 0x49, 0x2A, 0x00, 0x08, 0x00, 0x00, 0x00,
        0x01, 0x00,
        0x12, 0x01, 0x03, 0x00, 0x01, 0x00, 0x00, 0x00,
        orientation.toByte(), 0x00, 0x00, 0x00,
        0x00, 0x00, 0x00, 0x00,
        0xFF.toByte(), 0xD9.toByte()
    )

    private object ExifOrientation {
        const val FLIP_HORIZONTAL = 2
        const val ROTATE_90 = 6
    }
}
