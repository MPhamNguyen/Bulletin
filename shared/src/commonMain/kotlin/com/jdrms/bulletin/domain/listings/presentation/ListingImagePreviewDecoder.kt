package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.ui.graphics.ImageBitmap

internal expect fun decodeListingImagePreview(bytes: ByteArray): ImageBitmap?

internal data class ExifPreviewTransform(val rotationDegrees: Int, val isFlipped: Boolean)

internal fun ByteArray.exifPreviewTransform(): ExifPreviewTransform = when (readExifOrientation()) {
    2 -> ExifPreviewTransform(0, true)
    3 -> ExifPreviewTransform(180, false)
    4 -> ExifPreviewTransform(180, true)
    5 -> ExifPreviewTransform(270, true)
    6 -> ExifPreviewTransform(90, false)
    7 -> ExifPreviewTransform(90, true)
    8 -> ExifPreviewTransform(270, false)
    else -> ExifPreviewTransform(0, false)
}

private fun ByteArray.readExifOrientation(): Int? {
    if (size < 4 || unsigned(0) != JPEG_MARKER || unsigned(1) != START_OF_IMAGE) return null
    var segment = readJpegSegment(2)
    var orientation: Int? = null
    while (segment != null && orientation == null) {
        if (segment.marker == EXIF_MARKER && hasExifHeader(segment.payloadStart, segment.end)) {
            orientation = readTiffOrientation(segment.payloadStart + EXIF_HEADER_SIZE, segment.end)
        }
        segment = readJpegSegment(segment.end)
    }
    return orientation
}

private fun ByteArray.readJpegSegment(start: Int): JpegSegment? {
    var segment: JpegSegment? = null
    if (start + 4 <= size && unsigned(start) == JPEG_MARKER) {
        val marker = unsigned(start + 1)
        val segmentLength = readUnsignedShort(start + 2, bigEndian = true)
        val validEnd = segmentLength
            ?.takeIf { it >= MINIMUM_SEGMENT_LENGTH }
            ?.let { start + 2 + it }
            ?.takeIf { it <= size }
        if (marker != START_OF_SCAN && marker != END_OF_IMAGE && validEnd != null) {
            segment = JpegSegment(marker, payloadStart = start + 4, end = validEnd)
        }
    }
    return segment
}

private fun ByteArray.readTiffOrientation(tiffStart: Int, end: Int): Int? =
    readTiffDirectory(tiffStart, end)?.let { directory ->
        findOrientationEntry(directory.start, directory.entryCount, end, directory.bigEndian)
    }

private fun ByteArray.readTiffDirectory(tiffStart: Int, end: Int): TiffDirectory? {
    if (tiffStart + TIFF_HEADER_SIZE > end) return null
    val bigEndian = byteOrderAt(tiffStart) ?: return null
    val validMagic = readUnsignedShort(tiffStart + 2, bigEndian) == TIFF_MAGIC
    val firstIfdOffset = readUnsignedInt(tiffStart + 4, bigEndian)
    val ifdStart = firstIfdOffset?.let(tiffStart::plus)
    val validIfdStart = ifdStart?.takeIf { validMagic && it >= tiffStart && it + 2 <= end }
    val entryCount = validIfdStart?.let { readUnsignedShort(it, bigEndian) }
    return validIfdStart?.let { start -> entryCount?.let { TiffDirectory(start, it, bigEndian) } }
}

private fun ByteArray.findOrientationEntry(
    ifdStart: Int,
    entryCount: Int,
    end: Int,
    bigEndian: Boolean
): Int? {
    var orientation: Int? = null
    var index = 0
    while (index < entryCount && orientation == null) {
        val entry = ifdStart + 2 + index * IFD_ENTRY_SIZE
        if (entry + IFD_ENTRY_SIZE > end) break
        if (readUnsignedShort(entry, bigEndian) == ORIENTATION_TAG) {
            orientation = readUnsignedShort(entry + 8, bigEndian)?.takeIf { it in 1..8 }
        }
        index++
    }
    return orientation
}

private fun ByteArray.byteOrderAt(offset: Int): Boolean? = when {
    unsigned(offset) == MOTOROLA_BYTE_ORDER && unsigned(offset + 1) == MOTOROLA_BYTE_ORDER -> true
    unsigned(offset) == INTEL_BYTE_ORDER && unsigned(offset + 1) == INTEL_BYTE_ORDER -> false
    else -> null
}

private fun ByteArray.hasExifHeader(start: Int, end: Int): Boolean =
    start + EXIF_HEADER_SIZE <= end && EXIF_HEADER.indices.all { index -> this[start + index] == EXIF_HEADER[index] }

private fun ByteArray.readUnsignedShort(offset: Int, bigEndian: Boolean): Int? {
    if (offset < 0 || offset + 2 > size) return null
    val first = unsigned(offset)
    val second = unsigned(offset + 1)
    return if (bigEndian) (first shl 8) or second else first or (second shl 8)
}

private fun ByteArray.readUnsignedInt(offset: Int, bigEndian: Boolean): Int? {
    if (offset < 0 || offset + 4 > size) return null
    val values = IntArray(4) { index -> unsigned(offset + index) }
    val result = if (bigEndian) {
        (values[0] shl 24) or (values[1] shl 16) or (values[2] shl 8) or values[3]
    } else {
        values[0] or (values[1] shl 8) or (values[2] shl 16) or (values[3] shl 24)
    }
    return result.takeIf { it >= 0 }
}

private fun ByteArray.unsigned(index: Int): Int = this[index].toInt() and 0xFF

private data class JpegSegment(val marker: Int, val payloadStart: Int, val end: Int)
private data class TiffDirectory(val start: Int, val entryCount: Int, val bigEndian: Boolean)

private val EXIF_HEADER = byteArrayOf(0x45, 0x78, 0x69, 0x66, 0x00, 0x00)
private const val EXIF_HEADER_SIZE = 6
private const val MINIMUM_SEGMENT_LENGTH = 2
private const val TIFF_HEADER_SIZE = 8
private const val TIFF_MAGIC = 42
private const val IFD_ENTRY_SIZE = 12
private const val JPEG_MARKER = 0xFF
private const val START_OF_IMAGE = 0xD8
private const val START_OF_SCAN = 0xDA
private const val END_OF_IMAGE = 0xD9
private const val EXIF_MARKER = 0xE1
private const val MOTOROLA_BYTE_ORDER = 0x4D
private const val INTEL_BYTE_ORDER = 0x49
private const val ORIENTATION_TAG = 0x0112
