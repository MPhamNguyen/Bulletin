package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.cropSettings
import com.jdrms.bulletin.core.common.imageRequestUrl
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseInboxListingDto
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseInboxListingImageDto

internal fun inboxListingImage(url: String): SupabaseInboxListingDto {
    val crop = url.cropSettings()
    return SupabaseInboxListingDto(
        listOf(
            SupabaseInboxListingImageDto(url.imageRequestUrl(), 0, crop.scale, crop.offsetX, crop.offsetY)
        )
    )
}
