package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.EmailAlreadyInUseException
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
import kotlin.test.assertTrue

class SupabaseRepeatSignupTest {
    private val email = StudentEmail("student@example.com")
    private val alreadyInUse = "An account with this email address already exists. Please log in instead."

    private fun userJson(identities: String?) = buildString {
        append("""{"id":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee","aud":"authenticated",""")
        append(""""email":"student@example.com","email_confirmed_at":null,""")
        if (identities != null) append(""""identities":$identities,""")
        append(""""created_at":"2026-01-01T00:00:00Z"}""")
    }

    private val identity = """
        [{"identity_id":"11111111-2222-3333-4444-555555555555","id":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
        "user_id":"aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee","identity_data":{"email":"student@example.com"},
        "provider":"email","created_at":"2026-01-01T00:00:00Z"}]
    """.trimIndent()

    private suspend fun registerAgainst(body: String): Result<PendingRegistration> {
        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Auth) { minimalSettings() }
            httpEngine = MockEngine { request ->
                assertEquals("/auth/v1/signup", request.url.encodedPath)
                respond(body, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
            }
        }
        return try {
            SupabaseAuthRepository(client, InMemoryProfileRepository())
                .register(email, "password123", "Student Name", "CSULB")
                .also { assertNull(client.auth.currentSessionOrNull()) }
        } finally {
            client.close()
        }
    }

    @Test
    fun emptyIdentitiesReturnsEmailAlreadyInUseAndDoesNotAdvanceToVerification() = runTest {
        val error = assertIs<Result.Error>(registerAgainst(userJson("[]")))
        assertIs<EmailAlreadyInUseException>(error.exception)
        assertEquals(alreadyInUse, error.message)
    }

    @Test
    fun populatedIdentitiesForNewSignupContinueToVerification() = runTest {
        val result = registerAgainst(userJson(identity))
        assertEquals(email, assertIs<Result.Success<PendingRegistration>>(result).data.email)
    }

    @Test
    fun populatedIdentitiesForUnconfirmedSignupContinueToVerification() = runTest {
        val unconfirmed = userJson(identity).replace(
            "\"created_at\":\"2026-01-01T00:00:00Z\"}",
            "\"created_at\":\"2026-01-01T00:00:00Z\",\"confirmation_sent_at\":\"2026-01-02T00:00:00Z\"}"
        )
        val result = registerAgainst(unconfirmed)
        assertEquals(email, assertIs<Result.Success<PendingRegistration>>(result).data.email)
    }

    @Test
    fun nullIdentitiesPreserveCurrentBehavior() = runTest {
        val result = registerAgainst(userJson(null))
        assertEquals(email, assertIs<Result.Success<PendingRegistration>>(result).data.email)
    }

    @Test
    fun accountConflictErrorMapsToAlreadyInUseMessageWithoutExposingTheCode() {
        val message = SupabaseAuthRepository.mapAuthErrorMessage(
            Exception("P0001: bulletin_account_conflict")
        )
        assertEquals(alreadyInUse, message)
        assertTrue("bulletin_account_conflict" !in message)
    }
}
