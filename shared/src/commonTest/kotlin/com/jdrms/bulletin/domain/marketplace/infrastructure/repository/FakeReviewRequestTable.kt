package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequest
import com.jdrms.bulletin.domain.marketplace.domain.model.ReviewRequestStatus
import com.jdrms.bulletin.domain.marketplace.domain.repository.ReviewRequestRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.ReviewRequestDto
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * A deterministic stand-in for PostgREST's `listing_review_requests` table. It evaluates `eq.` and `in.(...)`
 * filters atomically, honours `Prefer: return=representation`, and records every request so tests can assert
 * how many round trips an operation used. [beforePatch] runs just before a PATCH is evaluated, which is how
 * tests model another client changing the row between the app's decision and its write.
 */
internal class FakeReviewRequestTable {
    val rows = mutableListOf<ReviewRequestDto>()
    val requests = mutableListOf<HttpRequestData>()
    var beforePatch: (() -> Unit)? = null
    var actingUserId: String? = null

    val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
        install(Postgrest)
        httpEngine = MockEngine { request ->
            requests += request
            val headers = headersOf("Content-Type", "application/json")
            when (request.method) {
                HttpMethod.Post -> {
                    upsert(request)
                    respond("", HttpStatusCode.Created, headers)
                }
                HttpMethod.Patch -> respond(patch(request), HttpStatusCode.OK, headers)
                else -> respond(Json.encodeToString(matching(request)), HttpStatusCode.OK, headers)
            }
        }
    }

    /** The repository under test, acting as whichever user the current operation belongs to. */
    fun repository(): ReviewRequestRepository = ActorAwareRepository(
        SupabaseReviewRequestRepository(client) { actingUserId },
        this
    )

    fun statusOf(listingId: String, buyerId: String): String? =
        rows.singleOrNull { it.listingId == listingId && it.buyerId == buyerId }?.status

    fun patches() = requests.filter { it.method == HttpMethod.Patch }

    fun statusFilter(request: HttpRequestData): Set<String> = request.url.parameters["status"].orEmpty()
        .substringAfter('.').trim('(', ')').split(',').map { it.trim('"') }.filter { it.isNotEmpty() }.toSet()

    private fun upsert(request: HttpRequestData) {
        val payload = Json.parseToJsonElement((request.body as TextContent).text)
        val items = if (payload is JsonArray) payload.map { it.jsonObject } else listOf(payload.jsonObject)
        items.map { Json.decodeFromJsonElement(ReviewRequestDto.serializer(), it) }.forEach { dto ->
            if (rows.none { it.listingId == dto.listingId && it.buyerId == dto.buyerId }) rows += dto
        }
    }

    private fun patch(request: HttpRequestData): String {
        beforePatch?.invoke()
        val newStatus = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
            .getValue("status").jsonPrimitive.content
        val updated = matching(request).map { it.copy(status = newStatus) }
        updated.forEach { row ->
            rows[rows.indexOfFirst { it.listingId == row.listingId && it.buyerId == row.buyerId }] = row
        }
        val wantsRows = request.headers["Prefer"].orEmpty().contains("return=representation")
        return if (wantsRows) Json.encodeToString(updated) else ""
    }

    private fun matching(request: HttpRequestData): List<ReviewRequestDto> {
        val params = request.url.parameters
        fun eq(name: String) = params[name]?.removePrefix("eq.")
        val status = params["status"]
        return rows.filter { row ->
            (eq("listing_id") ?: row.listingId) == row.listingId &&
                (eq("buyer_id") ?: row.buyerId) == row.buyerId &&
                (eq("seller_id") ?: row.sellerId) == row.sellerId &&
                when {
                    status == null -> true
                    status.startsWith("eq.") -> status.removePrefix("eq.") == row.status
                    else -> row.status in statusFilter(request)
                }
        }
    }

    private class ActorAwareRepository(
        private val delegate: ReviewRequestRepository,
        private val table: FakeReviewRequestTable
    ) : ReviewRequestRepository {
        override suspend fun createIfMissing(request: ReviewRequest): Result<Unit> {
            table.actingUserId = request.sellerId
            return delegate.createIfMissing(request)
        }

        override suspend fun pendingForBuyer(buyerId: String): List<ReviewRequest> {
            table.actingUserId = buyerId
            return delegate.pendingForBuyer(buyerId)
        }

        override suspend fun updateStatus(
            listingId: String,
            buyerId: String,
            status: ReviewRequestStatus
        ): Result<Unit> {
            table.actingUserId = buyerId
            return delegate.updateStatus(listingId, buyerId, status)
        }

        override suspend fun cancelForRestoredListing(listingId: String, sellerId: String): Result<Unit> {
            table.actingUserId = sellerId
            return delegate.cancelForRestoredListing(listingId, sellerId)
        }
    }
}
