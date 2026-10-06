package com.jdrms.bulletin.domain.profile

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.application.VerifyStudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.EmailVerificationOutcome
import com.jdrms.bulletin.domain.profile.domain.model.PendingRegistration
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.repository.ProfileRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.InMemoryProfileRepository
import com.jdrms.bulletin.domain.profile.infrastructure.repository.SupabaseAuthRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class SupabaseEmailVerificationTest {
    private val email = StudentEmail("student@example.com")

    @Test
    fun signupSendsMetadataAndReturnsPendingWithoutProfileIo() = runTest {
        var profileCalls = 0
        val profiles = object : ProfileRepository by InMemoryProfileRepository() {
            override suspend fun updateProfile(profile: StudentProfile): Result<StudentProfile> {
                profileCalls++
                return Result.Error(IllegalStateException("No authenticated session"))
            }
        }
        val client = client { path, body ->
            assertEquals("/auth/v1/signup", path)
            val request = Json.parseToJsonElement(body).jsonObject
            assertEquals(email.value, request["email"]?.jsonPrimitive?.content)
            assertEquals("Student Name", request["data"]?.jsonObject?.get("full_name")?.jsonPrimitive?.content)
            user(confirmed = false)
        }
        try {
            val repository = SupabaseAuthRepository(client, profiles)
            val result = repository.register(email, "password123", "Student Name", "CSULB")
            assertEquals(email, assertIs<Result.Success<PendingRegistration>>(result).data.email)
            assertEquals(0, profileCalls)
            assertNull(client.auth.currentSessionOrNull())
            assertNull((repository.getCurrentUser() as Result.Success).data)
        } finally {
            client.close()
        }
    }

    @Test
    fun verificationRequiresAnAuthoritativeProfileRow() = runTest {
        val client = client { path, body ->
            assertEquals("/auth/v1/verify", path)
            val request = Json.parseToJsonElement(body).jsonObject
            assertEquals("email", request["type"]?.jsonPrimitive?.content)
            assertEquals("012345", request["token"]?.jsonPrimitive?.content)
            assertEquals(email.value, request["email"]?.jsonPrimitive?.content)
            session()
        }
        try {
            val profiles = InMemoryProfileRepository(initialProfiles = emptyMap())
            val repository = SupabaseAuthRepository(client, profiles)
            val result = assertIs<Result.Success<EmailVerificationOutcome>>(
                VerifyStudentEmail(repository)(email, "012345")
            )
            assertIs<EmailVerificationOutcome.ProfileRecoveryRequired>(result.data)
            assertNull(profiles.getProfile(UserId(USER_ID)).getOrThrow())
            assertNull((repository.getCurrentUserId() as Result.Success).data)
        } finally {
            client.close()
        }
    }

    @Test
    fun resendsSignupCodeForTheRegisteredEmail() = runTest {
        val client = client { path, body ->
            assertEquals("/auth/v1/resend", path)
            val request = Json.parseToJsonElement(body).jsonObject
            assertEquals("signup", request["type"]?.jsonPrimitive?.content)
            assertEquals(email.value, request["email"]?.jsonPrimitive?.content)
            "{}"
        }
        try {
            assertIs<Result.Success<Unit>>(
                SupabaseAuthRepository(client, InMemoryProfileRepository()).resendVerificationCode(email)
            )
        } finally {
            client.close()
        }
    }

    @Test
    fun expiredCodeLeavesNoSessionAndShowsRecoverableError() = runTest {
        val client = client(status = HttpStatusCode.Forbidden) { _, _ ->
            """{"code":403,"error_code":"otp_expired","msg":"Token has expired or is invalid"}"""
        }
        try {
            val repository = SupabaseAuthRepository(client, InMemoryProfileRepository())
            val result = assertIs<Result.Error>(VerifyStudentEmail(repository)(email, "123456"))
            assertEquals(
                "This code is invalid or has expired. Request a new code and try again.",
                result.exception.message
            )
            assertNull(client.auth.currentSessionOrNull())
        } finally {
            client.close()
        }
    }

    @Test
    fun rejectsServerConfigurationThatAutoConfirmsSignup() = runTest {
        val client = client { path, _ -> if (path.endsWith("/logout")) "{}" else session() }
        try {
            val repository = SupabaseAuthRepository(client, InMemoryProfileRepository())
            assertIs<Result.Error>(repository.register(email, "password123", "Student Name", "CSULB"))
            assertNull(client.auth.currentSessionOrNull())
        } finally {
            client.close()
        }
    }

    @Test
    fun rejectsUnconfirmedVerificationResponse() = runTest {
        val client = client { path, _ -> if (path.endsWith("/logout")) "{}" else session(confirmed = false) }
        try {
            val repository = SupabaseAuthRepository(client, InMemoryProfileRepository())
            assertIs<Result.Error>(VerifyStudentEmail(repository)(email, "123456"))
            assertNull(client.auth.currentSessionOrNull())
            assertNull((repository.getCurrentUserId() as Result.Success).data)
        } finally {
            client.close()
        }
    }

    @Test
    fun clearsRejectedSessionEvenWhenRemoteSignOutFails() = runTest {
        val client = client { path, _ ->
            if (path.endsWith("/logout")) error("Unable to connect") else session(confirmed = false)
        }
        try {
            val repository = SupabaseAuthRepository(client, InMemoryProfileRepository())
            assertIs<Result.Error>(VerifyStudentEmail(repository)(email, "123456"))
            assertNull(client.auth.currentSessionOrNull())
        } finally {
            client.close()
        }
    }

    @Test
    fun doesNotRestoreOrLoginWithAnUnconfirmedIdentity() = runTest {
        val client = client { _, _ -> session(confirmed = false) }
        try {
            val repository = SupabaseAuthRepository(client, InMemoryProfileRepository())
            client.auth.signInWith(Email) {
                email = "student@example.com"
                password = "password123"
            }
            assertNull((repository.getCurrentUser() as Result.Success).data)
            assertNull((repository.getCurrentUserId() as Result.Success).data)
            assertIs<Result.Error>(repository.login(email, "password123"))
            assertNull(client.auth.currentSessionOrNull())
            assertNull((repository.getCurrentUser() as Result.Success).data)
            assertNull((repository.getCurrentUserId() as Result.Success).data)
        } finally {
            client.close()
        }
    }

    @Test
    fun deletedProfileInvalidatesThePersistedSessionAndUserId() = runTest {
        val client = client { path, _ -> if (path.endsWith("/logout")) "{}" else session() }
        val deletedProfile = StudentProfile(
            id = UserId(USER_ID),
            email = email,
            fullName = "Deleted Student",
            deleteAtMillis = 1_700_000_000_000L
        )
        val profiles = object : ProfileRepository by InMemoryProfileRepository() {
            override suspend fun getProfile(userId: UserId): Result<StudentProfile?> {
                return Result.Success(deletedProfile)
            }
        }
        try {
            val repository = SupabaseAuthRepository(client, profiles)
            client.auth.signInWith(Email) {
                this.email = "student@example.com"
                password = "password123"
            }

            val result = assertIs<Result.Error>(repository.getCurrentUserId())
            assertEquals("Account has been deleted.", result.exception.message)
            assertNull(client.auth.currentSessionOrNull())
            assertEquals(null, (repository.getCurrentUserId() as Result.Success).data)
        } finally {
            client.close()
        }
    }

    @Test
    fun loginClearsDeletedProfileSessionWhenRemoteSignOutFails() = runTest {
        val client = client { path, _ ->
            if (path.endsWith("/logout")) error("Unable to connect") else session()
        }
        val deletedProfile = StudentProfile(
            id = UserId(USER_ID),
            email = email,
            fullName = "Deleted Student",
            deleteAtMillis = 1_700_000_000_000L
        )
        val profiles = object : ProfileRepository by InMemoryProfileRepository() {
            override suspend fun getProfile(userId: UserId): Result<StudentProfile?> {
                return Result.Success(deletedProfile)
            }
        }
        try {
            val repository = SupabaseAuthRepository(client, profiles)
            assertIs<Result.Error>(repository.login(email, "password123"))
            assertNull(client.auth.currentSessionOrNull())
        } finally {
            client.close()
        }
    }

    @Test
    fun rejectsVerificationSessionForADifferentEmail() = runTest {
        val client = client { path, _ ->
            if (path.endsWith("/logout")) "{}" else session().replace("student@example.com", "other@example.com")
        }
        try {
            val repository = SupabaseAuthRepository(client, InMemoryProfileRepository())
            assertIs<Result.Error>(VerifyStudentEmail(repository)(email, "123456"))
            assertNull(client.auth.currentSessionOrNull())
        } finally {
            client.close()
        }
    }

    @Test
    fun profileFailureAfterConfirmationDoesNotExposeIdentity() = runTest {
        val client = client { _, _ -> session() }
        val profiles = object : ProfileRepository by InMemoryProfileRepository() {
            override suspend fun getProfile(userId: UserId): Result<StudentProfile?> {
                return Result.Error(IllegalStateException("Profile temporarily unavailable"))
            }
        }
        try {
            val repository = SupabaseAuthRepository(client, profiles)
            val result = assertIs<Result.Success<EmailVerificationOutcome>>(
                VerifyStudentEmail(repository)(email, "123456")
            )
            assertIs<EmailVerificationOutcome.ProfileRecoveryRequired>(result.data)
            assertIs<Result.Error>(repository.getCurrentUserId())
        } finally {
            client.close()
        }
    }

    @Test
    fun signupAndResendSurfaceProviderRateLimits() = runTest {
        val client = client(status = HttpStatusCode.TooManyRequests) { _, _ ->
            """{"error_code":"over_email_send_rate_limit","msg":"Email rate limit exceeded"}"""
        }
        try {
            val repository = SupabaseAuthRepository(client, InMemoryProfileRepository())
            val signup = assertIs<Result.Error>(repository.register(email, "password123", "Student Name", "CSULB"))
            val resend = assertIs<Result.Error>(repository.resendVerificationCode(email))
            assertEquals("Too many signup attempts. Please wait a few minutes before trying again.", signup.message)
            assertEquals(signup.message, resend.message)
            assertNull(client.auth.currentSessionOrNull())
        } finally {
            client.close()
        }
    }

    @Test
    fun cancellationIsNotConvertedToAuthenticationFailure() = runTest {
        val client = client { _, _ -> throw CancellationException("Cancelled") }
        try {
            val repository = SupabaseAuthRepository(client, InMemoryProfileRepository())
            assertFailsWith<CancellationException> { repository.resendVerificationCode(email) }
            assertFailsWith<CancellationException> {
                repository.register(email, "password123", "Student Name", "CSULB")
            }
            assertFailsWith<CancellationException> { VerifyStudentEmail(repository)(email, "123456") }
        } finally {
            client.close()
        }
    }

    private fun client(
        status: HttpStatusCode = HttpStatusCode.OK,
        response: (String, String) -> String
    ): SupabaseClient = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
        install(Auth) { minimalSettings() }
        httpEngine = MockEngine { request ->
            respond(
                response(request.url.encodedPath, (request.body as? TextContent)?.text.orEmpty()),
                status,
                headersOf("Content-Type", "application/json")
            )
        }
    }

    private fun user(confirmed: Boolean = true): String = """
        {
            "id":"$USER_ID", "aud":"authenticated", "email":"student@example.com",
            "email_confirmed_at":${if (confirmed) "\"2026-01-01T00:00:00Z\"" else "null"},
            "user_metadata":{"full_name":"Student Name","university":"CSULB"},
            "created_at":"2026-01-01T00:00:00Z"
        }
    """.trimIndent()

    private fun session(confirmed: Boolean = true): String = """
        {
            "access_token":"test-access-token", "refresh_token":"test-refresh-token",
            "token_type":"bearer", "expires_in":3600, "user":${user(confirmed)}
        }
    """.trimIndent()

    companion object {
        private const val USER_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
    }
}
