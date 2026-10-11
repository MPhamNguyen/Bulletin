package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.BoundedMemoryCache
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.StudentReputation
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

internal class ProfileReadCache(
    currentTimeMillis: () -> Long = ::profileCacheTimeMillis
) {
    private val sessionMutex = Mutex()
    private var sessionId: String? = null
    private val currentProfiles = BoundedMemoryCache<UserId, StudentProfile>(
        MAX_ENTRIES,
        TTL_MILLIS,
        currentTimeMillis
    )
    private val publicProfiles = BoundedMemoryCache<UserId, StudentProfile>(
        MAX_ENTRIES,
        TTL_MILLIS,
        currentTimeMillis
    )
    private val currentReputations = BoundedMemoryCache<UserId, StudentReputation>(
        MAX_ENTRIES,
        TTL_MILLIS,
        currentTimeMillis
    )
    private val publicReputations = BoundedMemoryCache<UserId, StudentReputation>(
        MAX_ENTRIES,
        TTL_MILLIS,
        currentTimeMillis
    )

    suspend fun profile(
        id: UserId,
        isCurrentUser: Boolean,
        loader: suspend () -> StudentProfile?
    ) = profileCache(isCurrentUser).getOrLoad(id, loader)

    suspend fun reputation(
        id: UserId,
        isCurrentUser: Boolean,
        loader: suspend () -> StudentReputation
    ) = reputationCache(isCurrentUser).getOrLoad(id, loader)!!

    suspend fun invalidate(id: UserId) {
        currentProfiles.invalidate(id)
        publicProfiles.invalidate(id)
        currentReputations.invalidate(id)
        publicReputations.invalidate(id)
    }

    suspend fun clear() {
        currentProfiles.clear()
        publicProfiles.clear()
        currentReputations.clear()
        publicReputations.clear()
    }

    suspend fun clearCurrentSession() {
        currentProfiles.clear()
        currentReputations.clear()
        sessionMutex.withLock { sessionId = null }
    }

    suspend fun observeSession(currentSessionId: String?) {
        sessionMutex.withLock {
            if (sessionId != currentSessionId) {
                currentProfiles.clear()
                currentReputations.clear()
                sessionId = currentSessionId
            }
        }
    }

    private fun profileCache(isCurrentUser: Boolean) =
        if (isCurrentUser) currentProfiles else publicProfiles

    private fun reputationCache(isCurrentUser: Boolean) =
        if (isCurrentUser) currentReputations else publicReputations

    companion object {
        const val TTL_MILLIS = 180_000L
        const val MAX_ENTRIES = 128
    }
}

private val profileClockStart = kotlin.time.TimeSource.Monotonic.markNow()
private fun profileCacheTimeMillis(): Long = profileClockStart.elapsedNow().inWholeMilliseconds
