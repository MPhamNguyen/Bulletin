package com.jdrms.bulletin.domain.listings.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.application.RestoreListingToMarketplace
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SupabaseListingRestoreWriteTest {
    @Test
    fun restoringSoldListingExplicitlyClearsBuyerThroughRpcAndOnReadback() = runTest {
        var stored = Json.parseToJsonElement(
            """{"id":"listing-1","name":"Desk","user_id":"seller","category":"FURNITURE",
                "condition":"GOOD","price":10.0,"description":"A desk","status":"SOLD","buyer_id":"buyer"}"""
        ).jsonObject
        var edit: JsonObject? = null
        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Postgrest)
            httpEngine = MockEngine { request ->
                val headers = headersOf("Content-Type", "application/json")
                if (request.url.encodedPath == "/rest/v1/rpc/edit_listing") {
                    val body = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
                    assertEquals(HttpMethod.Post, request.method)
                    edit = body.getValue("p_listing").jsonObject
                    stored = JsonObject(stored + checkNotNull(edit))
                    respond("", HttpStatusCode.NoContent, headers)
                } else {
                    respond("[$stored]", HttpStatusCode.OK, headers)
                }
            }
        }
        try {
            val repository = SupabaseListingsRepository(client)
            val result = RestoreListingToMarketplace(repository)(ListingId("listing-1"), SellerId("seller"))
            assertIs<Result.Success<*>>(result)
            val payload = checkNotNull(edit)
            assertEquals("AVAILABLE", payload.getValue("status").jsonPrimitive.content)
            assertTrue("buyer_id" in payload, "Restore must send buyer_id explicitly; omitting it retains the buyer")
            assertEquals(JsonNull, payload["buyer_id"])
            assertNull(repository.getListing(ListingId("listing-1"))?.buyerId)
        } finally {
            client.close()
        }
    }
}
