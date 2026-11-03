package com.jdrms.bulletin.domain.listings.attributes

import com.jdrms.bulletin.domain.listings.infrastructure.repository.SupabaseListingsRepository
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ListingAttributesPostgrestTest {
    @Test
    fun realClientIgnoresGeneratedColumnsAndReadsAttributesFromWildcardSelect() = runTest {
        val client = createSupabaseClient("https://attributes-test.supabase.co", "test-anon-key") {
            install(Postgrest)
            httpEngine = MockEngine { request ->
                assertEquals("/rest/v1/listings", request.url.encodedPath)
                respond(
                    """[{"id":"listing-1","name":"Laptop","user_id":"seller-1","price":10,
                    "category":"ELECTRONICS","attributes":{"brand":"Framework"},
                    "attr_brand_key":"framework","attr_ram_gb":null,"future_column":true}]""",
                    headers = headersOf("Content-Type", "application/json")
                )
            }
        }
        try {
            val row = SupabaseListingsRepository(client).getAllListings().single()
            assertEquals(laptopAttributes["brand"], row.attributes["brand"])
        } finally {
            client.close()
        }
    }
}
