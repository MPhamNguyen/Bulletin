package com.jdrms.bulletin.domain.messages.presentation

import androidx.compose.ui.unit.dp
import coil3.PlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Scale
import com.jdrms.bulletin.core.designsystem.theme.BulletinMotion

internal val MessageAvatarSize = 40.dp

/** Prefetch and rendering use identical decode dimensions and crop scale. */
internal fun messageAvatarImageRequest(context: PlatformContext, url: String, sizePixels: Int): ImageRequest =
    ImageRequest.Builder(context)
        .data(url)
        .size(sizePixels)
        .scale(Scale.FILL)
        .crossfade(BulletinMotion.FAST)
        .build()
