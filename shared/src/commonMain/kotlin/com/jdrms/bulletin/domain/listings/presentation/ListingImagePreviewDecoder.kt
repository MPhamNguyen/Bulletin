package com.jdrms.bulletin.domain.listings.presentation

import androidx.compose.ui.graphics.ImageBitmap

internal expect fun decodeListingImagePreview(bytes: ByteArray): ImageBitmap?
