package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.domain.profile.domain.model.ExternalSignInOutcome
import com.jdrms.bulletin.domain.profile.infrastructure.repository.ExternalAuthBrowser
import com.jdrms.bulletin.domain.profile.infrastructure.repository.ExternalAuthCallback
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.createSupabaseClient
import io.ktor.client.engine.mock.MockEngine
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class ExternalAuthWiringTest {
    @Test
    fun profileModuleResumesBrowserSignInAndClearsVerifierOnCancellation() = runTest {
        var browserCalled = false
        val browser = object : ExternalAuthBrowser {
            override val isResuming = true
            override suspend fun authorize(url: String): ExternalAuthCallback {
                assertEquals("", url)
                browserCalled = true
                return ExternalAuthCallback.Cancelled
            }
        }
        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Auth) {
                minimalSettings()
                flowType = FlowType.PKCE
                codeVerifierCache = MemoryCodeVerifierCache("pending-verifier")
            }
            httpEngine = MockEngine { error("Cancelled sign-in must not make a network request") }
        }
        try {
            val container = AppContainer(
                providedSupabaseClient = client,
                externalAuth = ExternalAuthConfiguration(microsoftSignInEnabled = true, browser = browser)
            )
            assertTrue(container.microsoftSignInEnabled)
            assertTrue(container.externalSignInPending)
            assertEquals(ExternalSignInOutcome.Cancelled, container.profile.signInExternally())
            assertTrue(browserCalled)
            assertNull(client.auth.codeVerifierCache.loadCodeVerifier())
            assertNull(client.auth.currentSessionOrNull())
        } finally {
            client.close()
        }
    }

    @Test
    fun signInViewModelBelongsToContainerAcrossRouteRecreation() {
        val container = AppContainer(isInspectionMode = true)
        val original = container.signInViewModel
        assertSame(original, container.signInViewModel)
    }

    @Test
    fun microsoftSignInIsDisabledByDefault() {
        val container = AppContainer(isInspectionMode = true)
        assertFalse(container.microsoftSignInEnabled)
        assertFalse(container.externalSignInPending)
    }
}
