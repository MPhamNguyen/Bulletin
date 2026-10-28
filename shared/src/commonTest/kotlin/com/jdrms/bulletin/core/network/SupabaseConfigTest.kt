package com.jdrms.bulletin.core.network

import io.github.jan.supabase.auth.AuthConfig
import io.github.jan.supabase.auth.FlowType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class SupabaseConfigTest {

    @Test
    fun ssoConfigurationPreservesSessionStorageAndEmailRedirectDefaults() {
        for (enabled in listOf(false, true)) {
            val auth = AuthConfig()
            val originalSessionManager = auth.sessionManager
            val originalFlow = auth.flowType
            val originalVerifierCache = auth.codeVerifierCache
            val storage = object : SupabaseAuthStorage {
                override suspend fun saveCodeVerifier(codeVerifier: String) = Unit
                override suspend fun loadCodeVerifier(): String? = null
                override suspend fun deleteCodeVerifier() = Unit
            }
            auth.configureBulletinAuth(storage, enabled)
            assertSame(originalSessionManager, auth.sessionManager)
            assertNull(auth.defaultRedirectUrl)
            assertEquals(if (enabled) FlowType.PKCE else originalFlow, auth.flowType)
            assertSame(if (enabled) storage else originalVerifierCache, auth.codeVerifierCache)
        }
    }

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
    fun testConfiguredFactoryTrimsTheUrlAndAnonymousKeyBeforeLoadingConfiguration() {
        val config = SupabaseConfig.configured(
            url = "  https://jdrms-project.supabase.co  ",
            apiKey = "  anon-key  "
        )

        assertTrue(config.isConfigured)
        assertEquals("https://jdrms-project.supabase.co", config.url)
        assertEquals("anon-key", config.apiKey)
    }

    @Test
    fun testBlankOrNullUrlOrKeyIsNotConfigured() {
        val nullConfig = SupabaseConfig(url = null, apiKey = null, isConnected = true)
        assertFalse(nullConfig.isConfigured)

        val blankUrl = SupabaseConfig(url = "", apiKey = "key123", isConnected = true)
        assertFalse(blankUrl.isConfigured)

        val blankKey = SupabaseConfig(url = "https://myproj.supabase.co", apiKey = "", isConnected = true)
        assertFalse(blankKey.isConfigured)

        val notConnected = SupabaseConfig(url = "https://myproj.supabase.co", apiKey = "key123", isConnected = false)
        assertFalse(notConnected.isConfigured)
    }
}
