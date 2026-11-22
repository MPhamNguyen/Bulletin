package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.ui.unit.dp
import coil3.PlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Scale
import com.jdrms.bulletin.core.common.imageRequestUrl
import com.jdrms.bulletin.core.designsystem.theme.BulletinMotion

internal val ConversationListingImageSize = 64.dp

/** Crop metadata affects rendering only, so warming and rendering share the same image cache key. */
internal fun conversationListingImageRequest(context: PlatformContext, url: String, sizePixels: Int): ImageRequest =
    ImageRequest.Builder(context)
        .data(url.imageRequestUrl())
        .size(sizePixels)
        .scale(Scale.FILL)
        .crossfade(BulletinMotion.FAST)
        .build()
