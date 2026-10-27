package com.jdrms.bulletin.core.common

data class ImageCropSettings(
    val scale: Float = 1f,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f
) {
    init {
        require(scale in 1f..4f && offsetX in -1f..1f && offsetY in -1f..1f)
    }
}

fun String.cropSettings(): ImageCropSettings {
    if (!contains("#bulletin_crop=")) return ImageCropSettings()
    val value = substringAfter("#bulletin_crop=")
    val parts = value.split(',')
    if (parts.size != 3) return ImageCropSettings()
    return runCatching {
        ImageCropSettings(parts[0].toFloat(), parts[1].toFloat(), parts[2].toFloat())
    }.getOrDefault(ImageCropSettings())
}

fun String.withCropSettings(crop: ImageCropSettings): String =
    substringBefore("#bulletin_crop=") + "#bulletin_crop=${crop.scale},${crop.offsetX},${crop.offsetY}"

fun String.imageRequestUrl(): String = substringBefore("#bulletin_crop=")
