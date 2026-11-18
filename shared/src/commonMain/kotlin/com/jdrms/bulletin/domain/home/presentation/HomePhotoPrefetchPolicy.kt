package com.jdrms.bulletin.domain.home.presentation

import com.jdrms.bulletin.core.common.imageRequestUrl
import com.jdrms.bulletin.domain.home.domain.model.HomeFeedItem

internal const val HOME_PHOTO_PREFETCH_LIMIT = 6

/**
 * Image URLs worth warming for the deck, ordered current card, next card, then later cards.
 * Mirrors [HomePhotoMedia]'s photo fallback and uses [imageRequestUrl] so cache keys match display requests.
 */
internal fun homePhotoPrefetchUrls(
    feed: List<HomeFeedItem>,
    currentIndex: Int,
    limit: Int = HOME_PHOTO_PREFETCH_LIMIT
): List<String> {
    if (limit <= 0 || currentIndex !in feed.indices) return emptyList()
    val urls = LinkedHashSet<String>()
    for (item in feed.subList(currentIndex, feed.size)) {
        for (url in item.photoUrls.ifEmpty { listOfNotNull(item.imageUrl) }) {
            urls += url.imageRequestUrl()
            if (urls.size >= limit) return urls.toList()
        }
    }
    return urls.toList()
}
