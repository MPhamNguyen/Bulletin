package com.jdrms.bulletin.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

data class SupabaseConfig(
    val url: String? = null,
    val apiKey: String? = null,
    val isConnected: Boolean = false
) {
    val isConfigured: Boolean
        get() = isConnected && !url.isNullOrBlank() && !apiKey.isNullOrBlank()

    fun createClient(): SupabaseClient {
        val resolvedUrl = checkNotNull(url) { "Supabase URL cannot be null when creating client." }
        val resolvedKey = checkNotNull(apiKey) { "Supabase API key cannot be null when creating client." }
        return createSupabaseClient(
            supabaseUrl = resolvedUrl,
            supabaseKey = resolvedKey
        ) {
            install(Auth)
            install(Postgrest)
        }
    }

    companion object {
        fun configured(url: String, apiKey: String): SupabaseConfig {
            return SupabaseConfig(
                url = url.trim(),
                apiKey = apiKey.trim(),
                isConnected = true
            )
        }
    }
}
