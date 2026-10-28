package com.jdrms.bulletin.domain.profile.infrastructure.repository

/** Platform boundary. Callback values remain inside infrastructure. */
interface ExternalAuthBrowser {
    val isResuming: Boolean get() = false
    fun clearRecovered() {}
    suspend fun authorize(url: String): ExternalAuthCallback
}

sealed interface ExternalAuthCallback {
    data class Code(val value: String) : ExternalAuthCallback
    data object Cancelled : ExternalAuthCallback
    data class Rejected(val reason: String?) : ExternalAuthCallback
}

object MicrosoftOAuthRedirect {
    const val URI = "com.jdrms.bulletin.auth://oauth/callback"
}
