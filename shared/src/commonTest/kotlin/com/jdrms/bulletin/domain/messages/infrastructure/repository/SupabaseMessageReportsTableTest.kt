package com.jdrms.bulletin.domain.messages.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.FakeSupabaseMessagesTable
import com.jdrms.bulletin.domain.messages.alice
import com.jdrms.bulletin.domain.messages.conversation
import com.jdrms.bulletin.domain.messages.conversationId
import com.jdrms.bulletin.domain.messages.domain.model.MessageCursor
import com.jdrms.bulletin.domain.messages.domain.model.MessageId
import com.jdrms.bulletin.domain.messages.domain.model.MessageReportAlreadySubmittedException
import com.jdrms.bulletin.domain.messages.infrastructure.dto.SupabaseMessageReportInsertDto
import com.jdrms.bulletin.domain.messages.infrastructure.mapper.SupabaseMessagesMapper
import com.jdrms.bulletin.domain.messages.message
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
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SupabaseMessageReportsTableTest {
    @Test
    fun insertsMessageReportIntoReportsWithNullDescriptionAndPlaceholderReason() = runTest {
        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Postgrest)
            httpEngine = MockEngine { request ->
                assertEquals("/rest/v1/reports", request.url.encodedPath)
                assertEquals(HttpMethod.Post, request.method)
                val payload = Json.parseToJsonElement(assertIs<TextContent>(request.body).text)
                val body = if (payload is JsonArray) payload.single().jsonObject else payload.jsonObject
                assertEquals("message", body.getValue("type").jsonPrimitive.content)
                assertEquals("message-a", body.getValue("message_id").jsonPrimitive.content)
                assertEquals("alice-id", body.getValue("reporter_id").jsonPrimitive.content)
                assertEquals("Inappropriate Content", body.getValue("reason").jsonPrimitive.content)
                assertEquals(JsonNull, body.getValue("description"))
                assertFalse("listing_id" in body)
                assertFalse("status" in body)
                respond("", HttpStatusCode.Created)
            }
        }
        try {
            val result = repository(client).reportMessage(alice.id, conversationId, message().id, "Spam")
            result.getOrThrow()
        } finally {
            client.close()
        }
    }

    @Test
    fun allMessageReadsJoinReportsAndPreserveReportedMessageMarkers() = runTest {
        val selections = mutableListOf<String>()
        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Postgrest)
            httpEngine = MockEngine { request ->
                assertEquals("/rest/v1/messages", request.url.encodedPath)
                selections += checkNotNull(request.url.parameters["select"])
                respond(REPORTED_MESSAGE, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
            }
        }
        try {
            val table = PostgrestSupabaseMessagesTable(client)
            val cursor = MessageCursor(message().orderingTimestamp, MessageId("older"))
            val rows = listOf(
                table.getMessagePage("conversation-a", null, 50).single(),
                table.getMessagePage("conversation-a", cursor, 50).single(),
                checkNotNull(table.findMessage("conversation-a", "message-a")),
                checkNotNull(table.findLatestMessage("conversation-a"))
            )
            assertEquals(4, selections.size)
            assertTrue(selections.all { "reports(id,status)" in it && "message_reports" !in it })
            assertTrue(rows.all { SupabaseMessagesMapper.toMessage(it).isReported })
        } finally {
            client.close()
        }
    }

    @Test
    fun reportBackendFailuresRemainSafeResultErrors() = runTest {
        listOf(
            HttpStatusCode.Unauthorized,
            HttpStatusCode.Forbidden,
            HttpStatusCode.NotFound,
            HttpStatusCode.Conflict
        ).forEach { status ->
            val client = failingClient(status)
            try {
                val result = repository(client).reportMessage(alice.id, conversationId, message().id, "Spam")
                assertIs<MessagesPersistenceException>(assertIs<Result.Error>(result).exception)
                assertEquals("Unable to access messages. Please try again.", result.message)
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun networkFailureSubmittingMessageReportReturnsSafeError() = runTest {
        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Postgrest)
            httpEngine = MockEngine { throw IOException("Network unavailable") }
        }
        try {
            val result = repository(client).reportMessage(alice.id, conversationId, message().id, "Spam")
            assertIs<MessagesPersistenceException>(assertIs<Result.Error>(result).exception)
        } finally {
            client.close()
        }
    }

    @Test
    fun duplicateMessageReportReturnsDomainFailure() = runTest {
        val client = failingClient(
            HttpStatusCode.Conflict,
            "23505",
            "duplicate key value violates unique constraint reports_active_message_unique"
        )
        try {
            val result = repository(client).reportMessage(alice.id, conversationId, message().id, "Spam")
            assertIs<MessageReportAlreadySubmittedException>(assertIs<Result.Error>(result).exception)
        } finally {
            client.close()
        }
    }

    @Test
    fun unrelatedUniqueConstraintViolationRemainsPersistenceFailure() = runTest {
        val client = failingClient(
            HttpStatusCode.Conflict,
            "23505",
            "duplicate key value violates unique constraint another_unique_constraint"
        )
        try {
            val result = repository(client).reportMessage(alice.id, conversationId, message().id, "Spam")
            assertIs<MessagesPersistenceException>(assertIs<Result.Error>(result).exception)
        } finally {
            client.close()
        }
    }

    private fun repository(client: SupabaseClient): SupabaseMessagesRepository {
        val transport = PostgrestSupabaseMessagesTable(client)
        val table = object : SupabaseMessagesTable by FakeSupabaseMessagesTable(
            listOf(conversation()),
            listOf(message())
        ) {
            override suspend fun insertReport(report: SupabaseMessageReportInsertDto) = transport.insertReport(report)
        }
        return SupabaseMessagesRepository(table)
    }

    private fun failingClient(
        status: HttpStatusCode,
        code: String = "42501",
        message: String = "private backend detail"
    ) =
        createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Postgrest)
            httpEngine = MockEngine {
                respond(
                    """{"code":"$code","message":"$message"}""",
                    status,
                    headersOf("Content-Type", "application/json")
                )
            }
        }

    private companion object {
        const val REPORTED_MESSAGE = """[{
            "id":"message-a", "conversation_id":"conversation-a", "sender_id":"alice-id",
            "body":"Hello", "created_at":"1970-01-01T00:00:00.042Z", "reports":[{"id":"report-a"}]
        }]"""
    }
}
