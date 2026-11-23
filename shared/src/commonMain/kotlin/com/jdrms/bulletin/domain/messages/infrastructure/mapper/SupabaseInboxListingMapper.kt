package com.jdrms.bulletin.domain.messages.infrastructure.mapper

import com.jdrms.bulletin.core.common.ImageCropSettings
import com.jdrms.bulletin.core.common.withCropSettings
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseInboxListingDto

internal fun SupabaseInboxListingDto.toThumbnailUrl(imageUrl: (String) -> String): String? {
    val cover = images.filter { it.objectPath.isNotBlank() }.minByOrNull { it.position } ?: return null
    val crop = runCatching { ImageCropSettings(cover.cropScale, cover.cropOffsetX, cover.cropOffsetY) }
        .getOrDefault(ImageCropSettings())
    return imageUrl(cover.objectPath).withCropSettings(crop)
}
