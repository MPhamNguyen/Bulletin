package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.core.network.SupabaseAuthStorage
import com.jdrms.bulletin.core.network.SupabaseConfig
import io.github.jan.supabase.SupabaseClient

internal class InfrastructureModule(
    private val supabaseConfig: SupabaseConfig,
    private val isInspectionMode: Boolean,
    private val allowInMemoryFallback: Boolean,
    private val providedSupabaseClient: SupabaseClient?,
    private val authStorage: SupabaseAuthStorage? = null,
    private val microsoftSignInEnabled: Boolean = false
) {
    val client: SupabaseClient? by lazy {
        if (providedSupabaseClient != null) {
            providedSupabaseClient
        } else if (!isInspectionMode && supabaseConfig.isConfigured) {
            runCatching {
                supabaseConfig.createClient(authStorage, microsoftSignInEnabled)
            }.fold(
                onSuccess = { it },
                onFailure = { error ->
                    println("Failed to initialize SupabaseClient: ${error.message}")
                    null
                }
            )
        } else {
            null
        }
    }

    fun <T> backend(supabase: (SupabaseClient) -> T, inMemory: () -> T): T {
        val configuredClient = client
        if (configuredClient != null) return supabase(configuredClient)
        check(allowInMemoryFallback || isInspectionMode) {
            "Supabase client is not configured and in-memory fallback is disabled."
        }
        return inMemory()
    }
}
