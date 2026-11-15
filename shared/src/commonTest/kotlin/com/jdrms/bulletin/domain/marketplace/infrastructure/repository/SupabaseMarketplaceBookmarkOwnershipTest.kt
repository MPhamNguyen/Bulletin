package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.OwnListingBookmarkException
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class SupabaseMarketplaceBookmarkOwnershipTest {
    @Test
    fun ownListingIsRejectedBeforeBookmarkInsert() = runBlocking {
        var requestCount = 0
        val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
            install(Postgrest)
            httpEngine = MockEngine {
                requestCount += 1
                respond(
                    """[{"user_id":"seller_101"}]""",
                    HttpStatusCode.OK,
                    headersOf("Content-Type", "application/json")
                )
            }
        }

        try {
            val result = SupabaseMarketplaceRepository(client)
                .bookmarkListing("seller_101", MarketplaceItemId("listing:mkt_1"))

            val error = assertIs<Result.Error>(result).exception
            assertIs<OwnListingBookmarkException>(
                error,
                "Unexpected bookmark failure: ${error.message}; cause=${error.cause?.message}"
            )
            assertEquals(1, requestCount)
        } finally {
            client.close()
        }
    }
}
