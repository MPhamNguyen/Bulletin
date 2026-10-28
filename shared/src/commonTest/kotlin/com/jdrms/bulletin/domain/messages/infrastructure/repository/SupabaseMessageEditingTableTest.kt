package com.jdrms.bulletin.domain.messages.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.messages.FakeSupabaseMessagesTable
import com.jdrms.bulletin.domain.messages.alice
import com.jdrms.bulletin.domain.messages.conversation
import com.jdrms.bulletin.domain.messages.conversationId
import com.jdrms.bulletin.domain.messages.domain.model.MessageEdit
import com.jdrms.bulletin.domain.messages.domain.model.MessageEditException
import com.jdrms.bulletin.domain.messages.domain.model.MessageEditFailure
import com.jdrms.bulletin.domain.messages.infrastructure.mapper.SupabaseMessagesMapper
import com.jdrms.bulletin.domain.messages.message
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Instant

class SupabaseMessageEditingTableTest {
    private val edit = MessageEdit(conversationId, message().id, "Changed", null)

    @Test
    fun rpcSendsExplicitNullVersionAndMapsServerTimestamp() = runTest {
        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Postgrest)
            httpEngine = MockEngine { request ->
                assertEquals(HttpMethod.Post, request.method)
                assertEquals("/rest/v1/rpc/edit_message", request.url.encodedPath)
                val body = Json.parseToJsonElement(assertIs<TextContent>(request.body).text).jsonObject
                assertEquals(JsonNull, body.getValue("p_expected_edited_at"))
                assertEquals("Changed", body.getValue("p_body").jsonPrimitive.content)
                assertEquals(conversationId.value, body.getValue("p_conversation_id").jsonPrimitive.content)
                assertEquals(message().id.value, body.getValue("p_message_id").jsonPrimitive.content)
                respond(EDITED_MESSAGE, HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
            }
        }
        try {
            val saved = SupabaseMessagesMapper.toMessage(PostgrestSupabaseMessagesTable(client).editMessage(edit))
            assertEquals("Changed", saved.content)
            assertEquals(Instant.parse("2026-10-26T10:01:00.000123Z"), saved.editedAt)
        } finally {
            client.close()
        }
    }

    @Test
    fun rpcFailuresBecomeSpecificEditErrors() = runTest {
        val codes = listOf("BM001", "BM002", "BM003", "BM004", "BM005")
        codes.zip(MessageEditFailure.entries).forEach { (code, expected) ->
            val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
                install(Postgrest)
                httpEngine = MockEngine {
                    respond(
                        """{"code":"$code","message":"Edit rejected","details":null,"hint":null}""",
                        HttpStatusCode.BadRequest, headersOf("Content-Type", "application/json")
                    )
                }
            }
            try {
                val reads = FakeSupabaseMessagesTable(listOf(conversation()))
                val table = object : SupabaseMessagesTable by reads {
                    override suspend fun editMessage(edit: MessageEdit) = PostgrestSupabaseMessagesTable(
                        client
                    ).editMessage(edit)
                }
                val result = SupabaseMessagesRepository(table).editMessage(alice.id, edit)
                assertEquals(expected, assertIs<MessageEditException>(assertIs<Result.Error>(result).exception).failure)
            } finally {
                client.close()
            }
        }
    }

    private companion object {
        const val EDITED_MESSAGE = """[{
            "id":"message-a","conversation_id":"conversation-a","sender_id":"alice-id",
            "body":"Changed","created_at":"2026-10-26T10:00:00Z",
            "edited_at":"2026-10-26T10:01:00.000123Z","deleted_at":null
        }]"""
    }
}
