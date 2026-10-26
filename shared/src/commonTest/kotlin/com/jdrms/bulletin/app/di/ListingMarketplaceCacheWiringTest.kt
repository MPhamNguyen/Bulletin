package com.jdrms.bulletin.app.di

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingCategory
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.ListingPrice
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.marketplace.application.MarketplacePageRequest
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

@OptIn(ExperimentalCoroutinesApi::class)
class ListingMarketplaceCacheWiringTest {
    @Test
    fun successfulListingWritesRefreshBothMarketplacePagesAndItems() = runTest {
        val fixture = ListingMarketplaceCacheFixture(writeSucceeds = true, StandardTestDispatcher(testScheduler))
        try {
            assertEquals("Original listing", fixture.pageTitle())
            assertEquals("Original listing", fixture.itemTitle())
            fixture.backendTitle = "Updated listing"
            assertEquals("Original listing", fixture.pageTitle())
            assertEquals("Original listing", fixture.itemTitle())
            assertEquals(2, fixture.listingReads)

            assertIs<Result.Success<Listing>>(fixture.createListing())

            assertEquals("Updated listing", fixture.pageTitle())
            assertEquals("Updated listing", fixture.itemTitle())
            assertEquals(4, fixture.listingReads)
        } finally {
            fixture.close()
        }
    }

    @Test
    fun failedListingWritesKeepTheExistingMarketplaceCache() = runTest {
        val fixture = ListingMarketplaceCacheFixture(writeSucceeds = false, StandardTestDispatcher(testScheduler))
        try {
            assertEquals("Original listing", fixture.pageTitle())
            assertEquals("Original listing", fixture.itemTitle())
            fixture.backendTitle = "Updated listing"

            assertIs<Result.Error>(fixture.createListing())

            assertEquals("Original listing", fixture.pageTitle())
            assertEquals("Original listing", fixture.itemTitle())
            assertEquals(2, fixture.listingReads)
        } finally {
            fixture.close()
        }
    }
}

private class ListingMarketplaceCacheFixture(writeSucceeds: Boolean, requestDispatcher: CoroutineDispatcher) {
    var backendTitle = "Original listing"
    var listingReads = 0
        private set
    private val client = createSupabaseClient("https://example.supabase.co", "test-anon-key") {
        install(Postgrest)
        httpEngine = MockEngine.create {
            dispatcher = requestDispatcher
            addHandler { request ->
                val headers = headersOf("Content-Type", "application/json")
                when {
                    request.method == HttpMethod.Post -> respond(
                        if (writeSucceeds) "{}" else """{"message":"Write rejected"}""",
                        if (writeSucceeds) HttpStatusCode.Created else HttpStatusCode.BadRequest,
                        headers
                    )
                    request.url.parameters["id"] == "eq.new-listing" -> respond("[]", HttpStatusCode.OK, headers)
                    else -> {
                        listingReads++
                        respond(
                            """[{"id":"existing","name":"$backendTitle","user_id":"seller","price":25.0}]""",
                            HttpStatusCode.OK,
                            headers
                        )
                    }
                }
            }
        }
    }
    private val container = AppContainer(providedSupabaseClient = client)

    suspend fun pageTitle(): String = container.marketplace.marketplaceListingSource
        .getAvailableListings(MarketplacePageRequest()).listings.single().title

    suspend fun itemTitle(): String? = container.marketplace.marketplaceRepository
        .getItem(MarketplaceItemId("existing"))?.title

    suspend fun createListing(): Result<Listing> = container.listings.createListing(
        Listing(
            id = ListingId("new-listing"),
            sellerId = SellerId("seller"),
            sellerName = "Student",
            title = "New listing",
            description = "A listing created through the listings module",
            price = ListingPrice(25.0),
            category = ListingCategory.OTHER
        )
    )

    suspend fun close() = client.close()
}
