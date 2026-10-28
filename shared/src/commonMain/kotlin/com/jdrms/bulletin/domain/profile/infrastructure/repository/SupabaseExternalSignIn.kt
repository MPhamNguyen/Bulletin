package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.ExternalSignInFailure
import com.jdrms.bulletin.domain.profile.domain.model.ExternalSignInOutcome
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Azure
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal class SupabaseExternalSignIn(
    private val supabase: SupabaseClient,
    private val browser: ExternalAuthBrowser,
    private val loadProfile: suspend () -> Result<StudentProfile?>,
    private val clearSession: suspend () -> Unit
) {
    private val mutex = Mutex()

    suspend fun signIn(): ExternalSignInOutcome {
        if (!mutex.tryLock()) return ExternalSignInOutcome.Failed(ExternalSignInFailure.Unavailable)
        try {
            return runCatching {
                supabase.auth.awaitInitialization()
                if (supabase.auth.currentSessionOrNull() != null) {
                    ExternalSignInOutcome.Failed(ExternalSignInFailure.AccountConflict)
                } else {
                    authenticate()
                }
            }.getOrElse { failure ->
                withContext(NonCancellable) { clearSession() }
                if (failure is CancellationException) throw failure
                ExternalSignInOutcome.Failed(mapFailure(failure.message))
            }
        } finally {
            browser.clearRecovered()
            withContext(NonCancellable) { supabase.auth.codeVerifierCache.deleteCodeVerifier() }
            mutex.unlock()
        }
    }

    private suspend fun authenticate(): ExternalSignInOutcome {
        val url = if (browser.isResuming) {
            ""
        } else {
            prepareOAuthUrl()
        }
        return when (val callback = browser.authorize(url)) {
            ExternalAuthCallback.Cancelled -> ExternalSignInOutcome.Cancelled
            is ExternalAuthCallback.Rejected -> ExternalSignInOutcome.Failed(mapCallbackFailure(callback.reason))
            is ExternalAuthCallback.Code -> exchange(callback.value)
        }
    }

    private suspend fun prepareOAuthUrl(): String {
        supabase.auth.codeVerifierCache.deleteCodeVerifier()
        val url = supabase.auth.getOAuthUrl(Azure, MicrosoftOAuthRedirect.URI) {
            scopes.addAll(listOf("email", "profile"))
            queryParams["prompt"] = "select_account"
        }
        // 3.1.4 launches the verifier write in its own scope. Wait for durable storage before redirecting.
        check(
            withTimeoutOrNull(5_000L) {
                while (supabase.auth.codeVerifierCache.loadCodeVerifier() == null) delay(10L)
                true
            } == true
        ) { "PKCE verifier could not be stored." }
        return url
    }

    private suspend fun exchange(code: String): ExternalSignInOutcome {
        val session = supabase.auth.exchangeCodeForSession(code, saveSession = false)
        val outcome = runCatching { acceptSession(session) }.getOrElse { failure ->
            withContext(NonCancellable) {
                runCatching { revokeRejectedSession(session.accessToken) }
                    .exceptionOrNull()?.let(failure::addSuppressed)
            }
            throw failure
        }
        if (outcome !is ExternalSignInOutcome.SignedIn) {
            withContext(NonCancellable) { revokeRejectedSession(session.accessToken) }
        }
        return outcome
    }

    private suspend fun acceptSession(session: UserSession): ExternalSignInOutcome {
        val user = supabase.auth.retrieveUser(session.accessToken)
        val rejection = MicrosoftIdentityPolicy.rejection(user)
        if (rejection != null) return ExternalSignInOutcome.Failed(rejection)
        // UX checks only: the identity trigger must enforce policy before Auth issues tokens.
        supabase.auth.importSession(session.copy(user = user, providerToken = null, providerRefreshToken = null))
        val profile = loadProfile()
        return if (profile is Result.Success && profile.data != null) {
            ExternalSignInOutcome.SignedIn(profile.data)
        } else {
            ExternalSignInOutcome.Failed(ExternalSignInFailure.ProfileUnavailable)
        }
    }

    private suspend fun revokeRejectedSession(accessToken: String) {
        try {
            // Despite the SDK namespace, this calls /logout with the user's JWT and anon key.
            // It does not require a service-role key or import the rejected session into app state.
            supabase.auth.admin.signOut(accessToken)
        } finally {
            clearSession()
        }
    }

    private fun mapFailure(message: String?): ExternalSignInFailure {
        val value = message.orEmpty().lowercase()
        return when {
            listOf(
                "connect",
                "timeout",
                "resolve host",
                "network"
            ).any(value::contains) -> ExternalSignInFailure.Network
            listOf("pkce", "code verifier", "flow_state", "auth code").any(value::contains) ->
                ExternalSignInFailure.InvalidCallback
            else -> ExternalSignInFailure.Technical
        }
    }

    private fun mapCallbackFailure(reason: String?): ExternalSignInFailure = when (reason) {
        "bulletin_unverified_email" -> ExternalSignInFailure.UnverifiedEmail
        "bulletin_account_conflict" -> ExternalSignInFailure.AccountConflict
        "bulletin_missing_email" -> ExternalSignInFailure.MissingEmail
        else -> ExternalSignInFailure.InvalidCallback
    }
}
