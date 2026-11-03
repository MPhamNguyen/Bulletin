package com.jdrms.bulletin.domain.profile.infrastructure.repository

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.user.UserInfo
import kotlinx.serialization.json.JsonPrimitive

/** Ends, rejects, and validates Supabase sessions so rejected credentials never outlive a failed check. */
internal class SupabaseAuthSessionGuard(
    private val supabase: SupabaseClient,
    private val onSessionEnded: suspend () -> Unit
) {

    suspend fun endSession() {
        supabase.auth.clearSession()
        onSessionEnded()
    }

    suspend fun signOut() {
        try {
            supabase.auth.signOut()
        } finally {
            endSession()
        }
    }

    suspend fun enforceExternalIdentity(user: UserInfo) {
        if (user.appMetadata?.get("provider") == JsonPrimitive("azure")) {
            if (MicrosoftIdentityPolicy.rejection(user) != null) {
                rejectSession("Microsoft account verification failed. Please sign in again.")
            }
        }
    }

    suspend fun rejectSession(message: String): Nothing {
        try {
            supabase.auth.signOut()
        } finally {
            // Rejected credentials must not survive a failed remote sign-out or an app restart.
            endSession()
        }
        error(message)
    }
}
