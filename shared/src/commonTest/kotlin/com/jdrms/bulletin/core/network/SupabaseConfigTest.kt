package com.jdrms.bulletin.core.network

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SupabaseConfigTest {

    @Test
    fun testDefaultConfigIsNotConfigured() {
        val config = SupabaseConfig()
        assertFalse(config.isConfigured)
    }

    @Test
    fun testConfiguredFactorySetsIsConfigured() {
        val config = SupabaseConfig.configured(
            url = "https://jdrms-project.supabase.co",
            apiKey = "real-anon-key-12345"
        )
        assertTrue(config.isConfigured)
        assertTrue(config.isConnected)
    }

    @Test
    fun testBlankUrlOrKeyIsNotConfigured() {
        val blankUrl = SupabaseConfig(url = "", apiKey = "key123", isConnected = true)
        assertFalse(blankUrl.isConfigured)

        val blankKey = SupabaseConfig(url = "https://myproj.supabase.co", apiKey = "", isConnected = true)
        assertFalse(blankKey.isConfigured)
    }
}
