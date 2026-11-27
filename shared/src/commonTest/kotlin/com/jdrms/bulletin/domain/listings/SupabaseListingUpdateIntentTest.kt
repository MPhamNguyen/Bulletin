package com.jdrms.bulletin.domain.listings

import com.jdrms.bulletin.domain.listings.attributes.attributeListing
import com.jdrms.bulletin.domain.listings.domain.model.ListingUpdateIntent
import com.jdrms.bulletin.domain.listings.infrastructure.dto.SupabaseListingEditRequest
import com.jdrms.bulletin.domain.listings.infrastructure.mapper.SupabaseListingMapper
import com.jdrms.bulletin.domain.listings.infrastructure.repository.SupabaseListingsRepository
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SupabaseListingUpdateIntentTest {
    private val listing = attributeListing().copy(createdAtMillis = 10L, editedAtMillis = 20L)

    @Test
    fun payloadsNeverCarryClientTimestampsOrRemovedFlags() {
        val dto = SupabaseListingMapper.toInsertDto(listing)
        listOf(Json.encodeToString(dto), Json.encodeToString(SupabaseListingEditRequest(dto))).forEach { body ->
            listOf("updated_at", "created_at", "edited_at", "row_version", "edit_requested").forEach { key ->
                assertFalse(key in body, "$key leaked into payload")
            }
            assertTrue("\"buyer_id\":null" in body)
            assertTrue("\"attributes\"" in body)
        }
    }

    @Test
    fun onlyEditsUseTheRpcAndRoutineWritesUseTheTable() = runTest {
        val writes = mutableListOf<String>()
        val engine = MockEngine { request ->
            if (request.method.value != "GET") writes += request.url.encodedPath
            respond(
                """[{"id":"listing-1","name":"Desk lamp","user_id":"seller-1","price":1.0}]""",
                HttpStatusCode.OK,
                headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = createSupabaseClient("https://example.supabase.co", "test-key") {
            install(Postgrest)
            httpEngine = engine
        }
        val repository = SupabaseListingsRepository(client)
        repository.updateListing(listing, ListingUpdateIntent.EDIT)
        repository.updateListing(listing, ListingUpdateIntent.ROUTINE)
        repository.updateListing(listing)
        assertEquals(
            listOf("/rest/v1/rpc/edit_listing", "/rest/v1/listings", "/rest/v1/listings"),
            writes
        )
        client.close()
    }
}
