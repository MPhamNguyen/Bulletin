package com.jdrms.bulletin.domain.profile.infrastructure.repository

/** A cold callback shares the original request deadline and can be consumed only once. */
internal class RecoveredExternalAuthCallback(private val now: () -> Long) {
    private var callback: ExternalAuthCallback? = null
    private var startedAt: Long = 0

    val isPending: Boolean
        get() {
            if (now() - startedAt !in 0..ExternalAuthCallbackReceiver.LIFETIME_MS) clear()
            return callback != null
        }

    fun store(value: ExternalAuthCallback, requestStartedAt: Long) {
        callback = value
        startedAt = requestStartedAt
    }

    fun consume(): ExternalAuthCallback? = if (isPending) callback.also { clear() } else null

    fun clear() {
        callback = null
    }
}
