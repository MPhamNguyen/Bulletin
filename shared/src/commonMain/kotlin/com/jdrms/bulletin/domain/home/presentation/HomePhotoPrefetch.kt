package com.jdrms.bulletin.domain.home.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalWindowInfo
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.size.Size

/** Warms Coil's caches for upcoming Home photos. Fire-and-forget so swipes never cancel downloads. */
@Composable
internal fun HomePhotoPrefetch(state: HomeUiState) {
    val context = LocalPlatformContext.current
    val window = LocalWindowInfo.current.containerSize
    val requested = remember(state.deckGeneration) { HashSet<String>() }
    LaunchedEffect(state.currentIndex, state.feed, state.deckGeneration, window) {
        if (window.width <= 0 || window.height <= 0) return@LaunchedEffect
        val loader = SingletonImageLoader.get(context)
        val size = Size(window.width, window.height)
        for (url in homePhotoPrefetchUrls(state.feed, state.currentIndex)) {
            if (requested.add(url)) {
                loader.enqueue(ImageRequest.Builder(context).data(url).size(size).build())
            }
        }
    }
}
