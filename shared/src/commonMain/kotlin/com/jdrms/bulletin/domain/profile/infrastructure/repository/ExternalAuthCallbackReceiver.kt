package com.jdrms.bulletin.domain.profile.infrastructure.repository

import io.ktor.http.Url

/** Only a pending, unexpired app request may consume a callback. PKCE remains SDK-owned. */
class ExternalAuthCallbackReceiver(
    private val startedAt: () -> Long?,
    private val clearPending: () -> Unit,
    private val now: () -> Long
) {
    fun receive(value: String): ExternalAuthCallback? {
        val uri = runCatching { Url(value) }.getOrNull() ?: return null
        if (!matches(uri) || '#' in value) return null
        val started = startedAt() ?: return null
        clearPending()
        return if (now() - started !in 0..LIFETIME_MS) ExternalAuthCallback.Rejected("expired") else parse(uri)
    }

    private fun matches(uri: Url): Boolean {
        val expected = Url(MicrosoftOAuthRedirect.URI)
        val destinationMatches = uri.protocol == expected.protocol && uri.host == expected.host &&
            uri.encodedPath == expected.encodedPath && uri.specifiedPort == expected.specifiedPort
        return destinationMatches && uri.encodedUser.isNullOrEmpty() && uri.encodedPassword.isNullOrEmpty()
    }

    private fun parse(uri: Url): ExternalAuthCallback {
        val code = uri.parameters.getAll("code").orEmpty()
        val error = uri.parameters.getAll("error").orEmpty()
        return when {
            code.size == 1 && code.single().isNotBlank() && error.isEmpty() -> ExternalAuthCallback.Code(code.single())
            code.isEmpty() && error.singleOrNull() == "access_denied" -> ExternalAuthCallback.Cancelled
            else -> ExternalAuthCallback.Rejected(knownReason(uri))
        }
    }

    private fun knownReason(uri: Url): String? {
        // Provider text can contain identifiers. Match only our fixed server rejection codes; never display it.
        val details = uri.parameters["error_description"].orEmpty()
        return listOf("bulletin_unverified_email", "bulletin_account_conflict", "bulletin_missing_email")
            .firstOrNull { it == uri.parameters["error_code"] || it in details }
    }

    companion object {
        const val LIFETIME_MS = 300_000L
    }
}
