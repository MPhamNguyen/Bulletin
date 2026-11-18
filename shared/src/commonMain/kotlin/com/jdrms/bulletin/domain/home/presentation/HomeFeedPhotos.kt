package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.LocalPlatformContext
import coil3.compose.SubcomposeAsyncImage
import coil3.compose.SubcomposeAsyncImageContent
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.jdrms.bulletin.core.common.cropSettings
import com.jdrms.bulletin.core.common.imageRequestUrl
import kotlin.math.min

@Composable
internal fun BoxScope.HomeFeedPhotos(photoUrl: String?, title: String) {
    photoUrl?.let { imageUrl ->
        val crop = imageUrl.cropSettings()
        SubcomposeAsyncImage(
            model = homeCrossfadeRequest(imageUrl.imageRequestUrl()),
            contentDescription = "$title listing photo",
            modifier = Modifier.matchParentSize(),
            contentScale = ContentScale.Fit
        ) {
            val imageScope = this
            val intrinsic = painter.intrinsicSize
            Box(Modifier.fillMaxSize()) {
                // Both layers share one request and transition, so the blur cannot load later.
                Image(
                    painter = imageScope.painter,
                    contentDescription = null,
                    modifier = Modifier.matchParentSize().scale(1.15f).blur(22.dp),
                    contentScale = ContentScale.Crop
                )
                imageScope.SubcomposeAsyncImageContent(
                    Modifier.matchParentSize().graphicsLayer {
                        val frameW = size.width
                        val frameH = size.height
                        val hasIntrinsic = intrinsic.width.isFinite() && intrinsic.height.isFinite() &&
                            intrinsic.width > 0f && intrinsic.height > 0f
                        val fit = if (hasIntrinsic) min(frameW / intrinsic.width, frameH / intrinsic.height) else 1f
                        val fittedW = if (hasIntrinsic) intrinsic.width * fit else frameW
                        val fittedH = if (hasIntrinsic) intrinsic.height * fit else frameH
                        val referenceSide = min(frameW, frameH)

                        scaleX = crop.scale
                        scaleY = crop.scale
                        translationX = cropAxisTranslation(crop.offsetX, crop.scale, frameW, referenceSide, fittedW)
                        translationY = cropAxisTranslation(crop.offsetY, crop.scale, frameH, referenceSide, fittedH)
                    }
                )
            }
        }
    }
}

@Composable
private fun homeCrossfadeRequest(url: String): ImageRequest {
    val context = LocalPlatformContext.current
    return remember(context, url) { ImageRequest.Builder(context).data(url).crossfade(true).build() }
}
