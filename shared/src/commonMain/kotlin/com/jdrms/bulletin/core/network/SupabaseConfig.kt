package com.jdrms.bulletin.core.network

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

data class SupabaseConfig(
    val url: String = "https://example.supabase.co",
    val apiKey: String = "public-anon-key",
    val isConnected: Boolean = false
) {
    val isConfigured: Boolean
        get() = (isConnected || (!url.contains("example.supabase.co") && apiKey != "public-anon-key")) &&
            url.isNotBlank() &&
            apiKey.isNotBlank()

    fun createClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = url,
            supabaseKey = apiKey
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
