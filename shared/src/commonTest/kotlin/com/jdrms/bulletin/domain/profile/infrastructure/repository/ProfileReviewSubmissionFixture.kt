package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.domain.profile.infrastructure.dto.ReviewDto
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.assertIs

/** Persists the HTTP insert body, deriving listing origin as the database does. */
internal class ProfileReviewSubmissionFixture {
    private val rows = mutableListOf<ReviewDto>()
    val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
        install(Auth) { minimalSettings() }
        install(Postgrest)
        httpEngine = MockEngine { request ->
            val body = when {
                request.url.encodedPath == "/auth/v1/token" ->
                    """{
                    "access_token":"test-access-token", "refresh_token":"test-refresh-token",
                    "token_type":"bearer", "expires_in":3600,
                    "user":{"id":"$FIRST_REVIEWER_ID", "aud":"authenticated", "email":"buyer@example.edu",
                        "created_at":"2026-01-01T00:00:00Z"}
                }"""
                request.url.encodedPath == "/rest/v1/reviews" && request.method == HttpMethod.Post -> {
                    val encoded = Json.parseToJsonElement(assertIs<TextContent>(request.body).text)
                    val payload = (if (encoded is JsonArray) encoded.single() else encoded).jsonObject
                    val listingId = payload["listing_id"]?.jsonPrimitive?.contentOrNull
                    rows += ReviewDto(
                        id = "stored-review",
                        reviewerId = payload.getValue("reviewer_id").jsonPrimitive.content,
                        revieweeId = payload.getValue("reviewee_id").jsonPrimitive.content,
                        score = payload.getValue("score").jsonPrimitive.int,
                        comment = payload.getValue("comment").jsonPrimitive.content,
                        listingId = listingId,
                        wasListingReview = listingId != null
                    )
                    "[]"
                }
                request.url.encodedPath == "/rest/v1/reviews" -> Json.encodeToString(rows)
                request.url.encodedPath == "/rest/v1/profiles" -> "[]"
                else -> error("Unexpected request: ${request.url.encodedPath}")
            }
            respond(body, headers = headersOf("Content-Type", "application/json"))
        }
    }
    val repository = SupabaseProfileRepository(client)

    suspend fun signIn() {
        client.auth.signInWith(Email) {
            email = "buyer@example.edu"
            password = "test-password"
        }
    }
}
