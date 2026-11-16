package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.infrastructure.dto.PublicProfileIdentityDto
import com.jdrms.bulletin.domain.profile.infrastructure.dto.ReviewDto
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal val revieweeId = UserId("00000000-0000-0000-0000-000000000001")
internal const val FIRST_REVIEWER_ID = "00000000-0000-0000-0000-000000000002"
internal const val SECOND_REVIEWER_ID = "00000000-0000-0000-0000-000000000003"

internal fun reviewRow(id: String = "review-1", reviewerId: String = FIRST_REVIEWER_ID) = ReviewDto(
    id = id,
    reviewerId = reviewerId,
    revieweeId = revieweeId.value,
    score = 5,
    comment = "Helpful and on time.",
    createdAt = "2026-10-25T12:00:00Z",
    listingId = "listing-1"
)

internal class ProfileReviewRepositoryFixture(
    rows: List<ReviewDto> = listOf(reviewRow()),
    profiles: List<PublicProfileIdentityDto> = listOf(PublicProfileIdentityDto(FIRST_REVIEWER_ID, "Campus Buyer")),
    failedTable: String? = null
) {
    val requests = mutableListOf<HttpRequestData>()
    val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
        install(Auth) { minimalSettings() }
        install(Postgrest)
        httpEngine = MockEngine { request ->
            requests += request
            val table = request.url.encodedPath.removePrefix("/rest/v1/")
            val body = when (table) {
                "reviews" -> Json.encodeToString(rows)
                "profiles" -> Json.encodeToString(profiles)
                else -> error("Unexpected table: $table")
            }
            respond(
                if (table == failedTable) """{"code":"XX000","message":"Read failed"}""" else body,
                if (table == failedTable) HttpStatusCode.ServiceUnavailable else HttpStatusCode.OK,
                headersOf("Content-Type", "application/json")
            )
        }
    }
    val repository = SupabaseProfileRepository(client)
}
