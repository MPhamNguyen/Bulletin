package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.BoundedMemoryCache
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlin.time.TimeSource

/** Shared bounded URL cache; a successful absence is cached, while loader failures are retried. */
internal class PublicProfilePhotoCache(
    currentTimeMillis: () -> Long = { clockStart.elapsedNow().inWholeMilliseconds }
) {
    private data class Photo(val url: String?)
    private val entries = BoundedMemoryCache<UserId, Photo>(MAX_ENTRIES, TTL_MILLIS, currentTimeMillis)

    suspend fun getOrLoad(userId: UserId, loader: suspend () -> String?): String? =
        entries.getOrLoad(userId) { Photo(loader()) }?.url

    suspend fun getIfPresent(userId: UserId): String? = entries.getIfPresent(userId)?.url

    suspend fun invalidate(userId: UserId) = entries.invalidate(userId)

    suspend fun clear() = entries.clear()

    companion object {
        const val MAX_ENTRIES = 200
        const val TTL_MILLIS = 300_000L
        private val clockStart = TimeSource.Monotonic.markNow()
    }
}
