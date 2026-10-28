package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.ExternalSignInFailure
import com.jdrms.bulletin.domain.profile.domain.model.ExternalSignInOutcome
import com.jdrms.bulletin.domain.profile.domain.model.StudentEmail
import com.jdrms.bulletin.domain.profile.domain.model.StudentProfile
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.FlowType
import io.github.jan.supabase.auth.MemoryCodeVerifierCache
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.createSupabaseClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.Url
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
import kotlin.test.assertTrue

class SupabaseExternalSignInTest {
    @Test
    fun startsAzureWithPkceProfileScopesAndAccountPicker() = runTest {
        val client = client()
        try {
            val browser = object : ExternalAuthBrowser {
                override suspend fun authorize(url: String): ExternalAuthCallback {
                    val parameters = Url(url).parameters
                    assertEquals("azure", parameters["provider"])
                    assertEquals("email profile", parameters["scopes"])
                    assertEquals("select_account", parameters["prompt"])
                    assertEquals("s256", parameters["code_challenge_method"]?.lowercase())
                    assertEquals(MicrosoftOAuthRedirect.URI, parameters["redirect_to"])
                    assertTrue(!parameters["code_challenge"].isNullOrBlank())
                    assertTrue(!client.auth.codeVerifierCache.loadCodeVerifier().isNullOrBlank())
                    return ExternalAuthCallback.Cancelled
                }
            }
            val flow = SupabaseExternalSignIn(client, browser, { error("No profile expected") }, {})
            assertEquals(ExternalSignInOutcome.Cancelled, flow.signIn())
            assertNull(client.auth.codeVerifierCache.loadCodeVerifier())
        } finally { client.close() }
    }

    @Test
    fun exchangesPkceWithoutPersistingProviderTokensAndReusesProfile() = runTest {
        val client = client()
        var loads = 0
        val flow = SupabaseExternalSignIn(client, browser(), {
            loads++
            Result.Success(profile)
        }, {
            client.auth.clearSession()
        })
        try {
            assertIs<ExternalSignInOutcome.SignedIn>(flow.signIn())
            assertEquals(profile.id.value, client.auth.currentUserOrNull()?.id)
            assertNull(client.auth.currentSessionOrNull()?.providerToken)
            assertNull(client.auth.codeVerifierCache.loadCodeVerifier())
            assertEquals(1, loads)
        } finally { client.close() }
    }

    @Test
    fun existingSessionConflictDiscardsRecoveredCallbackBeforeSignOut() = runTest {
        val client = client()
        try {
            val initial = SupabaseExternalSignIn(client, browser(), { Result.Success(profile) }, {})
            assertIs<ExternalSignInOutcome.SignedIn>(initial.signIn())
            var pending = true
            val recovered = object : ExternalAuthBrowser {
                override val isResuming: Boolean get() = pending
                override fun clearRecovered() { pending = false }
                override suspend fun authorize(url: String): ExternalAuthCallback = error("Must not consume code")
            }
            val flow = SupabaseExternalSignIn(client, recovered, { error("No profile expected") }, {})
            assertEquals(ExternalSignInOutcome.Failed(ExternalSignInFailure.AccountConflict), flow.signIn())
            assertEquals(false, recovered.isResuming)
            assertNull(client.auth.codeVerifierCache.loadCodeVerifier())
        } finally { client.close() }
    }

    @Test
    fun passwordLoginAndSignOutDiscardRecoveryEvenOnNetworkFailure() = runTest {
        val client = client(failure = "Unable to connect to network")
        var clears = 0
        val recovered = object : ExternalAuthBrowser {
            override fun clearRecovered() { clears++ }
            override suspend fun authorize(url: String): ExternalAuthCallback = error("No browser expected")
        }
        try {
            val repository = SupabaseAuthRepository(client, InMemoryProfileRepository(), recovered)
            repository.login(StudentEmail("student@school.edu"), "password")
            assertEquals(1, clears)
            repository.signOut()
            assertEquals(2, clears)
        } finally { client.close() }
    }

    @Test
    fun missingOrUnverifiedClaimsAndEmailCollisionNeverEstablishSession() = runTest {
        val cases = listOf(
            user(verified = false) to ExternalSignInFailure.UnverifiedEmail,
            user(email = null) to ExternalSignInFailure.MissingEmail,
            user(otherIdentity = true) to ExternalSignInFailure.AccountConflict,
            user(confirmed = false) to ExternalSignInFailure.UnverifiedEmail
        )
        for ((response, reason) in cases) {
            val client = client(response)
            try {
                val flow = SupabaseExternalSignIn(client, browser(), { error("Profile must not be loaded") }, {
                    client.auth.clearSession()
                })
                assertEquals(ExternalSignInOutcome.Failed(reason), flow.signIn())
                assertNull(client.auth.currentSessionOrNull())
            } finally { client.close() }
        }
    }

    @Test
    fun cancelAndInvalidCallbackLeaveNoSessionOrVerifier() = runTest {
        for (callback in listOf(ExternalAuthCallback.Cancelled, ExternalAuthCallback.Rejected("expired"))) {
            val client = client()
            try {
                val flow = SupabaseExternalSignIn(client, browser(callback), { error("No profile expected") }, {})
                val expected = if (callback == ExternalAuthCallback.Cancelled) {
                    ExternalSignInOutcome.Cancelled
                } else {
                    ExternalSignInOutcome.Failed(ExternalSignInFailure.InvalidCallback)
                }
                assertEquals(expected, flow.signIn())
                assertNull(client.auth.currentSessionOrNull())
                assertNull(client.auth.codeVerifierCache.loadCodeVerifier())
            } finally { client.close() }
        }
    }

    @Test
    fun expiredCodeAndNetworkFailuresAreTypedAndDoNotExposeProviderText() = runTest {
        for (failure in listOf("flow_state_expired", "Unable to connect to network")) {
            val client = client(failure = failure)
            try {
                val flow = SupabaseExternalSignIn(client, browser(), { error("No profile expected") }, {
                    client.auth.clearSession()
                })
                val expected = if (failure == "flow_state_expired") {
                    ExternalSignInFailure.InvalidCallback
                } else {
                    ExternalSignInFailure.Network
                }
                assertEquals(ExternalSignInOutcome.Failed(expected), flow.signIn())
                assertNull(client.auth.currentSessionOrNull())
            } finally { client.close() }
        }
    }

    @Test
    fun profileFailureClearsSessionAndCanBeRetried() = runTest {
        val client = client()
        var fail = true
        val flow = SupabaseExternalSignIn(client, browser(), {
            if (fail) Result.Error(IllegalStateException("Profile unavailable")) else Result.Success(profile)
        }, { client.auth.clearSession() })
        try {
            assertEquals(ExternalSignInOutcome.Failed(ExternalSignInFailure.ProfileUnavailable), flow.signIn())
            assertNull(client.auth.currentSessionOrNull())
            fail = false
            client.auth.codeVerifierCache.saveCodeVerifier("test-verifier")
            assertIs<ExternalSignInOutcome.SignedIn>(flow.signIn())
        } finally { client.close() }
    }

    @Test
    fun coroutineCancellationClearsSessionAndPropagates() = runTest {
        val client = client()
        try {
            val browser = object : ExternalAuthBrowser {
                override val isResuming = true
                override suspend fun authorize(url: String): ExternalAuthCallback = throw CancellationException()
            }
            val flow = SupabaseExternalSignIn(
                client,
                browser,
                { Result.Success(profile) },
                { client.auth.clearSession() }
            )
            assertFailsWith<CancellationException> { flow.signIn() }
            assertNull(client.auth.currentSessionOrNull())
        } finally { client.close() }
    }

    private fun browser(callback: ExternalAuthCallback = ExternalAuthCallback.Code("one-time-code")) =
        object : ExternalAuthBrowser {
            // Cold callbacks must retain the original PKCE verifier rather than generate a second one.
            override val isResuming = true
            override suspend fun authorize(url: String): ExternalAuthCallback {
                assertEquals("", url)
                return callback
            }
        }

    private fun client(user: String = user(), failure: String? = null) =
        createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Auth) {
                minimalSettings()
                flowType = FlowType.PKCE
                codeVerifierCache = MemoryCodeVerifierCache("test-verifier")
            }
            httpEngine = MockEngine { request ->
                if (failure == "Unable to connect to network") error(failure)
                val body = if (request.url.encodedPath.endsWith("/token")) {
                    val json = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                    assertEquals("test-verifier", json["code_verifier"]?.jsonPrimitive?.content)
                    assertEquals("one-time-code", json["auth_code"]?.jsonPrimitive?.content)
                    session(user)
                } else {
                    user
                }
                respond(
                    if (failure != null) """{"error_code":"$failure","msg":"$failure"}""" else body,
                    if (failure != null) HttpStatusCode.BadRequest else HttpStatusCode.OK,
                    headersOf("Content-Type", "application/json")
                )
            }
        }

    private fun user(
        verified: Boolean = true,
        email: String? = "student@outlook.com",
        otherIdentity: Boolean = false,
        confirmed: Boolean = true
    ): String = """
        {"id":"$USER_ID","aud":"authenticated","email":${email?.let { "\"$it\"" } ?: "null"},
        "email_confirmed_at":${if (confirmed) "\"2026-01-01T00:00:00Z\"" else "null"},
        "app_metadata":{"provider":"azure"},
        "user_metadata":{"email_verified":true,"university":"Untrusted school"},
        "identities":[{"id":"ms-subject","provider":"azure","user_id":"$USER_ID",
        "identity_data":{"email":"student@outlook.com","email_verified":$verified}}
        ${if (otherIdentity) ",{\"id\":\"password\",\"provider\":\"email\",\"user_id\":\"$USER_ID\",\"identity_data\":{}}" else ""}]}
    """.trimIndent()

    private fun session(user: String) = """
        {"access_token":"test-access","refresh_token":"test-refresh","provider_token":"discard-me",
        "token_type":"bearer","expires_in":3600,"user":$user}
    """.trimIndent()

    companion object {
        private const val USER_ID = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee"
        private val profile = StudentProfile(UserId(USER_ID), StudentEmail("student@outlook.com"), "Student")
    }
}
