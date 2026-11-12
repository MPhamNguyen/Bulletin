package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.domain.profile.domain.model.UserId
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SupabaseReputationSummaryTest {
    @Test
    fun summaryUsesAggregateRpcInsteadOfFetchingReviewRows() = runTest {
        val userId = "c3a81234-5678-4abc-9def-123456789abc"
        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Auth) { minimalSettings() }
            install(Postgrest)
            httpEngine = MockEngine { request ->
                assertEquals(HttpMethod.Post, request.method)
                assertEquals("/rest/v1/rpc/get_reputation_summary", request.url.encodedPath)
                val parameters = Json.parseToJsonElement(assertIs<TextContent>(request.body).text).jsonObject
                assertEquals(userId, parameters["p_reviewee_id"]?.toString()?.trim('"'))
                respond(
                    """[{"average_rating":4.25,"total_reviews":8}]""",
                    headers = headersOf("Content-Type", "application/json")
                )
            }
        }

        try {
            val summary = SupabaseProfileRepository(client).getReputationSummary(UserId(userId))

            assertEquals(4.25, summary.averageRating)
            assertEquals(8, summary.totalReviews)
            assertTrue(summary.reviews.isEmpty())
        } finally {
            client.close()
        }
    }
}
