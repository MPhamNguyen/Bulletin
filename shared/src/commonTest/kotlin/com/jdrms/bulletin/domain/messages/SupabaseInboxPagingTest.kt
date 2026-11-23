package com.jdrms.bulletin.domain.messages

import com.jdrms.bulletin.domain.messages.infrastructure.repository.PostgrestSupabaseMessagesTable
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SupabaseInboxPagingTest {
    @Test
    fun pagesAndTargetedRefreshesFilterAtTheBackend() = runTest {
        var page = true
        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Postgrest)
            httpEngine = MockEngine { request ->
                assertTrue("listing_is_sold" in request.url.parameters["select"].orEmpty().split(','))
                assertTrue(request.url.parameters["select"].orEmpty().contains("profiles(full_name,avatar_url)"))
                assertTrue(request.url.parameters["select"].orEmpty().contains("listing:listings(listing_images("))
                assertEquals("1", request.url.parameters["listing.listing_images.limit"])
                assertEquals("position.asc.nullslast", request.url.parameters["listing.listing_images.order"])
                assertEquals("eq.alice-id", request.url.parameters["membership.user_id"])
                assertEquals("is.null", request.url.parameters["membership.deleted_at"])
                if (page) {
                    assertEquals("10", request.url.parameters["offset"])
                    assertEquals("10", request.url.parameters["limit"])
                    assertEquals("updated_at.desc.nullslast,id.desc.nullslast", request.url.parameters["order"])
                } else {
                    assertEquals("eq.conversation-a", request.url.parameters["id"])
                }
                respond("[]", HttpStatusCode.OK, headersOf("Content-Type", "application/json"))
            }
        }
        try {
            val table = PostgrestSupabaseMessagesTable(client)
            table.getInboxPage(alice.id.value, 10, 10)
            page = false
            table.getInboxConversation(alice.id.value, conversationId.value)
        } finally {
            client.close()
        }
    }
}
