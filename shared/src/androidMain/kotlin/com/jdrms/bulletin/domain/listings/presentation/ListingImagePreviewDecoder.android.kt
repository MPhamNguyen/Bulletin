package com.jdrms.bulletin.domain.listings.presentation

import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

internal actual fun decodeListingImagePreview(bytes: ByteArray): ImageBitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

    var sampleSize = 1
    while (maxOf(bounds.outWidth, bounds.outHeight) / sampleSize > MAX_PREVIEW_DIMENSION) {
        sampleSize *= 2
    }
    val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options) ?: return null
    val transform = bytes.exifPreviewTransform()
    return applyExifTransform(decoded, transform).asImageBitmap()
}

private fun applyExifTransform(
    bitmap: android.graphics.Bitmap,
    transform: ExifPreviewTransform
): android.graphics.Bitmap {
    if (transform.rotationDegrees == 0 && !transform.isFlipped) return bitmap
    val matrix = Matrix().apply {
        if (transform.isFlipped) postScale(-1f, 1f)
        if (transform.rotationDegrees != 0) postRotate(transform.rotationDegrees.toFloat())
    }
    return android.graphics.Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
}

private const val MAX_PREVIEW_DIMENSION = 2_048
