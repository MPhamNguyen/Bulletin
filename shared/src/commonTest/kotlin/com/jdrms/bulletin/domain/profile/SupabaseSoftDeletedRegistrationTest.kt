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

class SupabaseSoftDeletedRegistrationTest {
    private val email = StudentEmail("deleted@example.com")

    @Test
    fun registerWhenUserAlreadyExistsSendsOTPAndSucceeds() = runTest {
        val profiles = InMemoryProfileRepository(initialProfiles = emptyMap())
        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Auth) { minimalSettings() }
            httpEngine = MockEngine { request ->
                val path = request.url.encodedPath
                if (path.contains("signup")) {
                    // Simulate "user_already_exists" error from Supabase Auth
                    respond(
                        """{"error_code":"user_already_exists","msg":"User already registered"}""",
                        HttpStatusCode.Conflict,
                        headersOf("Content-Type", "application/json")
                    )
                } else {
                    // RPC (reactivate_soft_deleted_profile), OTP, and other calls succeed
                    respond("{}", HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
                }
            }
        }

        try {
            val authRepo = SupabaseAuthRepository(client, profiles)
            val result = authRepo.register(email, "password123", "New Name", "CSULB")

            assertIs<Result.Success<PendingRegistration>>(result)
            assertEquals(email, result.data.email)
            assertNull(client.auth.currentSessionOrNull())
        } finally {
            client.close()
        }
    }

    @Test
    fun registerWhenFakeUserReturnedSendsOTPAndSucceeds() = runTest {
        // When both email and phone confirmation are enabled, Supabase returns a fake user with empty
        // identities instead of throwing an error. The code throws EmailAlreadyInUseException, which
        // is now caught: the profile is reactivated via RPC and a fresh OTP email is sent.
        val profiles = InMemoryProfileRepository(initialProfiles = emptyMap())
        val fakeUserJson = """{"id":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee","aud":"authenticated",
            "email":"deleted@example.com","email_confirmed_at":null,"identities":[],
            "created_at":"2026-01-01T00:00:00Z"}"""

        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Auth) { minimalSettings() }
            httpEngine = MockEngine { request ->
                val path = request.url.encodedPath
                if (path.contains("signup")) {
                    // Simulate fake user with empty identities (existing confirmed user)
                    respond(fakeUserJson, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
                } else {
                    // RPC (reactivate_soft_deleted_profile), OTP, and other calls succeed
                    respond("{}", HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
                }
            }
        }

        try {
            val authRepo = SupabaseAuthRepository(client, profiles)
            val result = authRepo.register(email, "password123", "New Name", "CSULB")

            assertIs<Result.Success<PendingRegistration>>(result)
            assertEquals(email, result.data.email)
            assertNull(client.auth.currentSessionOrNull())
        } finally {
            client.close()
        }
    }

    @Test
    fun registerWithoutExistingUserProceedsNormally() = runTest {
        val profiles = InMemoryProfileRepository(initialProfiles = emptyMap())
        val identity = """
            [{"identity_id":"11111111-2222-3333-4444-555555555555","id":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
            "user_id":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee","identity_data":{"email":"deleted@example.com"},
            "provider":"email","created_at":"2026-01-01T00:00:00Z"}]
        """.trimIndent()

        val userJson = """{"id":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee","aud":"authenticated",
            "email":"deleted@example.com","email_confirmed_at":null,"identities":$identity,
            "created_at":"2026-01-01T00:00:00Z"}"""

        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Auth) { minimalSettings() }
            httpEngine = MockEngine { request ->
                respond(userJson, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
            }
        }

        try {
            val authRepo = SupabaseAuthRepository(client, profiles)
            val result = authRepo.register(email, "password123", "New Name", "CSULB")

            assertIs<Result.Success<PendingRegistration>>(result)
            assertEquals(email, result.data.email)
            assertNull(client.auth.currentSessionOrNull())
        } finally {
            client.close()
        }
    }
}
