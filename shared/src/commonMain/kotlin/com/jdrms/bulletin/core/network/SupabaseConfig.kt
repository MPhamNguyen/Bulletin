package com.jdrms.bulletin.core.network

data class SupabaseConfig(
    val url: String = "https://example.supabase.co",
    val apiKey: String = "public-anon-key",
    val isConnected: Boolean = true
)
