package com.jdrms.bulletin.domain.profile.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import com.jdrms.bulletin.domain.profile.domain.model.UserReport
import com.jdrms.bulletin.domain.profile.domain.model.UserReportAlreadySubmittedException
import com.jdrms.bulletin.domain.profile.domain.model.UserReportReason
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class SupabaseUserReportsTableTest {
    @Test
    fun insertsUserReportIntoReportsPreservingReasonAndDescription() = runTest {
        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Postgrest)
            httpEngine = MockEngine { request ->
                assertEquals("/rest/v1/reports", request.url.encodedPath)
                assertEquals(HttpMethod.Post, request.method)
                val payload = Json.parseToJsonElement(assertIs<TextContent>(request.body).text)
                val body = if (payload is JsonArray) payload.single().jsonObject else payload.jsonObject
                assertEquals("user", body.getValue("type").jsonPrimitive.content)
                assertEquals("reporter-id", body.getValue("reporter_id").jsonPrimitive.content)
                assertEquals("target-id", body.getValue("reported_user_id").jsonPrimitive.content)
                assertEquals("Scam or Fraud", body.getValue("reason").jsonPrimitive.content)
                assertEquals("Details", body.getValue("description").jsonPrimitive.content)
                assertFalse("message_id" in body)
                assertFalse("listing_id" in body)
                assertFalse("status" in body)
                respond("", HttpStatusCode.Created)
            }
        }
        try {
            repository(client).submit(report("  Details  ")).getOrThrow()
        } finally {
            client.close()
        }
    }

    @Test
    fun duplicateReportFromReportsTableKeepsExistingFailure() = runTest {
        val client = failingClient(HttpStatusCode.Conflict, "23505")
        try {
            val result = repository(client).submit(report())
            assertIs<UserReportAlreadySubmittedException>(assertIs<Result.Error>(result).exception)
        } finally {
            client.close()
        }
    }

    @Test
    fun reportBackendFailuresKeepExistingSafeError() = runTest {
        listOf(HttpStatusCode.Unauthorized, HttpStatusCode.Forbidden, HttpStatusCode.NotFound).forEach { status ->
            val client = failingClient(status, "42501")
            try {
                val result = assertIs<Result.Error>(repository(client).submit(report()))
                assertEquals("We couldn't submit your report right now. Please try again.", result.message)
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun networkFailureSubmittingUserReportReturnsSafeError() = runTest {
        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Postgrest)
            httpEngine = MockEngine { throw IOException("Network unavailable") }
        }
        try {
            val result = assertIs<Result.Error>(repository(client).submit(report()))
            assertEquals("We couldn't submit your report right now. Please try again.", result.message)
        } finally {
            client.close()
        }
    }

    private fun repository(client: SupabaseClient): SupabaseUserReportRepository {
        val table = object : SupabaseUserReportsTable by PostgrestSupabaseUserReportsTable(client) {
            override suspend fun authenticatedUserId() = "reporter-id"
        }
        return SupabaseUserReportRepository(table)
    }

    private fun report(description: String = "") = UserReport.create(
        UserId("reporter-id"),
        UserId("target-id"),
        UserReportReason.SCAM_OR_FRAUD,
        description
    ).getOrThrow()

    private fun failingClient(status: HttpStatusCode, code: String) =
        createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Postgrest)
            httpEngine = MockEngine {
                respond(
                    """{"code":"$code","message":"private backend detail"}""",
                    status,
                    headersOf("Content-Type", "application/json")
                )
            }
        }
}
