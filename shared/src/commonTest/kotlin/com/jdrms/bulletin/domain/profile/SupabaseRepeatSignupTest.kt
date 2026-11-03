package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.PendingRegistration
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.SupabaseAuthRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.createSupabaseClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class SupabaseRepeatSignupTest {
    @Test
    fun repeatSignupObfuscatedResponseIsIndistinguishableFromANewSignup() = runTest {
        val email = StudentEmail("student@example.com")
        // A confirmed account's repeat signup can return an obfuscated user without identities or a session.
        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Auth) { minimalSettings() }
            httpEngine = MockEngine { request ->
                assertEquals("/auth/v1/signup", request.url.encodedPath)
                respond(
                    """
                    {
                        "id":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", "aud":"authenticated",
                        "email":"student@example.com", "email_confirmed_at":null,
                        "identities":[], "created_at":"2026-01-01T00:00:00Z"
                    }
                    """.trimIndent(),
                    HttpStatusCode.OK,
                    headersOf("Content-Type", "application/json")
                )
            }
        }
        try {
            val repository = SupabaseAuthRepository(client, InMemoryProfileRepository())
            val result = repository.register(email, "password123", "Student Name", "CSULB")
            assertEquals(email, assertIs<Result.Success<PendingRegistration>>(result).data.email)
            assertNull(client.auth.currentSessionOrNull())
        } finally {
            client.close()
        }
    }
}
