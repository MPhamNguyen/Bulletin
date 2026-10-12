package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.application.MessagesInstrumentation
import com.jdrms.bulletin.domain.messages.application.MessagesTimingEvent
import com.jdrms.bulletin.domain.messages.domain.model.Conversation
import com.jdrms.bulletin.domain.messages.infrastructure.repository.SupabaseMessagesRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.minimalSettings
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SupabaseMessagesInboxTimingTest {
    @Test
    fun recordsRequestPayloadDecodeAndMappingStages() = runTest {
        val payload = """
            [{"id":"conversation-a","updated_at":"1970-01-01T00:00:00.001Z",
            "participants":[{"user_id":"alice-id","profiles":{"full_name":"Alice"}}]}]
        """.trimIndent()
        val client = client(payload)
        val events = mutableListOf<MessagesTimingEvent>()
        try {
            val result = SupabaseMessagesRepository(client, MessagesInstrumentation(events::add))
                .getConversations(alice.id)

            assertEquals(1, assertIs<Result.Success<List<Conversation>>>(result).data.size)
            assertEquals(payload.encodeToByteArray().size, events.singleFor("inbox_request_body").responseBytes)
            assertEquals(1, events.singleFor("inbox_decode").resultCount)
            assertEquals(1, events.singleFor("inbox_mapping").resultCount)
            assertEquals(1, events.singleFor("inbox_retrieval").resultCount)
            assertEquals(4, events.size)
        } finally {
            client.close()
        }
    }

    @Test
    fun malformedResponseRecordsDecodeFailureAndNoMapping() = runTest {
        val client = client("not-json")
        val events = mutableListOf<MessagesTimingEvent>()
        try {
            val result = SupabaseMessagesRepository(client, MessagesInstrumentation(events::add))
                .getConversations(alice.id)

            assertIs<Result.Error>(result)
            assertEquals("success", events.singleFor("inbox_request_body").outcome)
            assertEquals("failure", events.singleFor("inbox_decode").outcome)
            assertEquals("failure", events.singleFor("inbox_retrieval").outcome)
            assertEquals(3, events.size)
        } finally {
            client.close()
        }
    }

    private fun List<MessagesTimingEvent>.singleFor(operation: String): MessagesTimingEvent =
        single { it.operation == operation }

    private fun client(payload: String): SupabaseClient =
        createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Auth) { minimalSettings() }
            install(Postgrest)
            httpEngine = MockEngine {
                respond(payload, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
            }
        }
}
