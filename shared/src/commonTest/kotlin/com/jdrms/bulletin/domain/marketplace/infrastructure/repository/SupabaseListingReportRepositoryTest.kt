package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingReport
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingReportAlreadySubmittedException
import com.jdrms.bulletin.domain.reporting.domain.model.ReportReason
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingReportSelfReportException
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReporterId
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SupabaseListingReportRepositoryTest {
    @Test
    fun treatsOnlyClosedStatusesAsAllowingAnotherReport() {
        val missingStatus: String? = null
        assertTrue("open".isOpenListingReportStatus())
        assertTrue("in_review".isOpenListingReportStatus())
        assertTrue(missingStatus.isOpenListingReportStatus())
        assertFalse("closed".isOpenListingReportStatus())
        assertFalse("resolved".isOpenListingReportStatus())
        assertFalse("dismissed".isOpenListingReportStatus())
    }

    @Test
    fun mapsInsertPermissionFailureToGenericUserFacingFailureAndKeepsCause() {
        val cause = IllegalStateException("permission denied")

        val result = ListingReportPersistenceFailureMapper.map("42501", cause)

        assertEquals(SupabaseListingReportRepository.SAFE_FAILURE_MESSAGE, result.message)
        assertEquals(cause, result.cause)
    }

    @Test
    fun mapsUnsupportedReportTypeToGenericUserFacingFailureAndKeepsCause() {
        val cause = IllegalStateException("enum mismatch")
        val result = ListingReportPersistenceFailureMapper.map("22P02", cause)

        assertEquals(SupabaseListingReportRepository.SAFE_FAILURE_MESSAGE, result.message)
        assertEquals(cause, result.cause)
    }

    @Test
    fun mapsDatabaseSelfReportRejectionToPlainUserFacingMessageAndKeepsCause() {
        val cause = IllegalStateException("listing reporter matches owner")

        val result = ListingReportPersistenceFailureMapper.map("23514", cause)

        assertIs<ListingReportSelfReportException>(result)
        assertEquals("You cannot report your own listing.", result.message)
        assertEquals(cause, result.cause)
    }

    @Test
    fun mapsDuplicateSubmissionToDuplicateReportFailure() {
        assertIs<ListingReportAlreadySubmittedException>(
            ListingReportPersistenceFailureMapper.map("23505", IllegalStateException("duplicate"))
        )
    }

    @Test
    fun keepsUnknownPersistenceFailureSafeAndGeneric() {
        val cause = IllegalStateException("sensitive backend detail")

        val result = ListingReportPersistenceFailureMapper.map("XX000", cause)

        assertEquals(SupabaseListingReportRepository.SAFE_FAILURE_MESSAGE, result.message)
        assertEquals(cause, result.cause)
    }

    @Test
    fun insertsListingReportIntoCurrentReportsTable() = runTest {
        var insertSeen = false
        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Postgrest)
            httpEngine = MockEngine { request ->
                assertEquals("/rest/v1/reports", request.url.encodedPath)
                if (request.method == HttpMethod.Get) {
                    respond("[]", HttpStatusCode.OK)
                } else {
                    insertSeen = true
                    assertEquals(HttpMethod.Post, request.method)
                    val payload = Json.parseToJsonElement((request.body as TextContent).text)
                    val body = if (payload is JsonArray) payload.single().jsonObject else payload.jsonObject
                    assertEquals("listing", body.getValue("type").jsonPrimitive.content)
                    assertEquals("reporter-id", body.getValue("reporter_id").jsonPrimitive.content)
                    assertEquals("listing-id", body.getValue("listing_id").jsonPrimitive.content)
                    assertEquals("Scam or Fraud", body.getValue("reason").jsonPrimitive.content)
                    assertEquals(JsonNull, body.getValue("description"))
                    respond("", HttpStatusCode.Created)
                }
            }
        }
        try {
            assertIs<Result.Success<Unit>>(repository(client).submit(report()))
            assertEquals(true, insertSeen)
        } finally {
            client.close()
        }
    }

    @Test
    fun doesNotInsertWhenReporterAlreadyReportedListing() = runTest {
        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Postgrest)
            httpEngine = MockEngine { request ->
                assertEquals(HttpMethod.Get, request.method)
                respond("[{\"status\":\"open\"}]", HttpStatusCode.OK)
            }
        }
        try {
            val result = repository(client).submit(report())

            assertIs<ListingReportAlreadySubmittedException>(assertIs<Result.Error>(result).exception)
        } finally {
            client.close()
        }
    }

    @Test
    fun allowsNewReportAfterPriorReportWasClosed() = runTest {
        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Postgrest)
            httpEngine = MockEngine { request ->
                if (request.method == HttpMethod.Get) {
                    respond("[{\"status\":\"resolved\"}]", HttpStatusCode.OK)
                } else {
                    respond("", HttpStatusCode.Created)
                }
            }
        }
        try {
            assertIs<Result.Success<Unit>>(repository(client).submit(report()))
        } finally {
            client.close()
        }
    }

    @Test
    fun forgedSellerIdCannotBypassDatabaseSelfReportConstraint() = runTest {
        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Postgrest)
            httpEngine = MockEngine { request ->
                if (request.method == HttpMethod.Get) {
                    respond("[]", HttpStatusCode.OK)
                } else {
                    respond(
                        "{\"code\":\"23514\",\"message\":\"self report rejected\"}",
                        HttpStatusCode.BadRequest
                    )
                }
            }
        }
        try {
            val forgedSeller = report().copy(listingSellerId = "different-seller")

            val result = assertIs<Result.Error>(repository(client).submit(forgedSeller))

            assertIs<ListingReportSelfReportException>(result.exception)
            assertEquals("You cannot report your own listing.", result.message)
            assertTrue(result.exception.cause != null)
        } finally {
            client.close()
        }
    }

    @Test
    fun openReportReturnsListingIdForHiding() = runTest {
        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Postgrest)
            httpEngine = MockEngine {
                respond("[{\"listing_id\":\"listing-id\",\"status\":\"open\"}]", HttpStatusCode.OK)
            }
        }
        try {
            val ids = assertIs<Result.Success<Set<MarketplaceItemId>>>(
                repository(client).reportedListingIds(MarketplaceReporterId("reporter-id"))
            ).data

            assertEquals(setOf(MarketplaceItemId("listing:listing-id")), ids)
        } finally {
            client.close()
        }
    }

    @Test
    fun closedReportDoesNotReturnListingIdForHiding() = runTest {
        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Postgrest)
            httpEngine = MockEngine {
                respond("[{\"listing_id\":\"listing-id\",\"status\":\"resolved\"}]", HttpStatusCode.OK)
            }
        }
        try {
            val ids = assertIs<Result.Success<Set<MarketplaceItemId>>>(
                repository(client).reportedListingIds(MarketplaceReporterId("reporter-id"))
            ).data

            assertEquals(emptySet(), ids)
        } finally {
            client.close()
        }
    }

    @Test
    fun rejectsReporterIdentityMismatchBeforeInsert() = runTest {
        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Postgrest)
            httpEngine = MockEngine { error("Unexpected insert") }
        }
        try {
            val table = object : SupabaseListingReportsTable by PostgrestSupabaseListingReportsTable(client) {
                override suspend fun authenticatedUserId() = "different-user"
            }
            val result = assertIs<Result.Error>(SupabaseListingReportRepository(table).submit(report()))
            assertEquals("Report identity does not match the signed-in user.", result.message)
        } finally {
            client.close()
        }
    }

    @Test
    fun networkFailureReturnsSafeResultError() = runTest {
        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Postgrest)
            httpEngine = MockEngine { throw IOException("Network unavailable") }
        }
        try {
            val result = assertIs<Result.Error>(repository(client).submit(report()))
            assertEquals(SupabaseListingReportRepository.SAFE_FAILURE_MESSAGE, result.message)
        } finally {
            client.close()
        }
    }

    private fun repository(client: SupabaseClient) = SupabaseListingReportRepository(
        object : SupabaseListingReportsTable by PostgrestSupabaseListingReportsTable(client) {
            override suspend fun authenticatedUserId() = "reporter-id"
        }
    )

    private fun report() = ListingReport(
        listingId = MarketplaceItemId("listing:listing-id"),
        listingSellerId = "seller-id",
        reporterId = MarketplaceReporterId("reporter-id"),
        reason = ReportReason.SCAM_OR_FRAUD,
        description = ""
    )
}
