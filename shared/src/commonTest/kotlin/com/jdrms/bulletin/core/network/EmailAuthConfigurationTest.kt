package com.jdrms.bulletin.core.network

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.SupabaseAuthRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.createSupabaseClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class EmailAuthConfigurationTest {
    @Test
    fun signupKeepsServerEmailRedirectWithAndWithoutSso() = runTest {
        for (enabled in listOf(false, true)) {
            var requests = 0
            val client = createSupabaseClient("https://email-regression.supabase.co", "test-anon-key") {
                install(Auth) {
                    minimalSettings()
                    configureBulletinAuth(null, enabled)
                }
                httpEngine = MockEngine { request ->
                    requests++
                    assertEquals("/auth/v1/signup", request.url.encodedPath)
                    assertNull(request.url.parameters["redirect_to"])
                    respond(user, headers = headersOf("Content-Type", "application/json"))
                }
            }
            try {
                val repository = SupabaseAuthRepository(client, InMemoryProfileRepository())
                assertIs<Result.Success<*>>(repository.register(email, "password123", "Student", "School"))
                assertEquals(1, requests)
                assertNull(client.auth.currentSessionOrNull())
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun passwordResetKeepsServerEmailRedirectWithAndWithoutSso() = runTest {
        for (enabled in listOf(false, true)) {
            var requests = 0
            val client = createSupabaseClient("https://email-regression.supabase.co", "test-anon-key") {
                install(Auth) {
                    minimalSettings()
                    configureBulletinAuth(null, enabled)
                }
                httpEngine = MockEngine { request ->
                    requests++
                    assertEquals("/auth/v1/recover", request.url.encodedPath)
                    assertNull(request.url.parameters["redirect_to"])
                    respond("{}", headers = headersOf("Content-Type", "application/json"))
                }
            }
            try {
                val repository = SupabaseAuthRepository(client, InMemoryProfileRepository())
                assertIs<Result.Success<*>>(repository.requestPasswordReset(email))
                assertEquals(1, requests)
                assertNull(client.auth.currentSessionOrNull())
            } finally {
                client.close()
            }
        }
    }

    companion object {
        private val email = StudentEmail("student@school.edu")
        private const val user = """{"id":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
            "aud":"authenticated","email":"student@school.edu","identities":[]}"""
    }
}
