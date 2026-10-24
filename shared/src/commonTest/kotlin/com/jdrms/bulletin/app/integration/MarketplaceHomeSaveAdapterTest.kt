package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.home.application.SaveListingFailure
import com.jdrms.bulletin.domain.marketplace.application.BookmarkMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.repository.MarketplaceRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import io.github.jan.supabase.exceptions.RestException
import io.github.jan.supabase.postgrest.exception.PostgrestRestException
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNull

class MarketplaceHomeSaveAdapterTest {
    @Test
    fun successAndRetryUseTheOriginalUserAndListingIdEveryTime() = runTest {
        val f = HomeSaveFixture()
        f.failure = IOException("offline")
        assertIs<SaveListingFailure.Network>(assertIs<Result.Error>(f.adapter.save("viewer", actionListingId)).exception)
        f.failure = null
        assertIs<Result.Success<Unit>>(f.adapter.save("viewer", actionListingId))
        assertEquals(List(2) { "viewer" to MarketplaceItemId(actionListingId.value) }, f.calls)
    }

    @Test
    fun mapsEveryMarketplaceFailureToASafeHomeFailure() = runTest {
        val cases = listOf(
            NoSuchElementException("private listing") to SaveListingFailure.ListingUnavailable::class,
            IOException("private URL") to SaveListingFailure.Network::class,
            Exception("Bookmark request timed out.") to SaveListingFailure.Network::class,
            IllegalStateException("private backend data") to SaveListingFailure.Unknown::class
        )
        cases.forEach { (error, expected) ->
            val f = HomeSaveFixture().apply { failure = error }
            val failure = assertIs<Result.Error>(f.adapter.save("viewer", actionListingId)).exception
            assertEquals(expected, failure::class)
            assertNull(failure.message)
            assertNull(failure.cause)
        }
    }

    @Test
    fun mapsSupabaseStatusAndMissingListingForeignKeyFailures() = runTest {
        val cases = listOf(
            401 to SaveListingFailure.NotSignedIn::class,
            404 to SaveListingFailure.ListingUnavailable::class,
            408 to SaveListingFailure.Network::class,
            429 to SaveListingFailure.Network::class,
            503 to SaveListingFailure.Network::class,
            403 to SaveListingFailure.Unknown::class,
            400 to SaveListingFailure.Unknown::class
        )
        cases.forEach { (status, expected) ->
            val client = HttpClient(MockEngine { respond("", HttpStatusCode.fromValue(status)) })
            try {
                val response = client.get("https://example.test")
                val f = HomeSaveFixture().apply { failure = RestException("private", null, response) }
                assertEquals(expected, assertIs<Result.Error>(f.adapter.save("viewer", actionListingId)).exception::class)
                f.failure = PostgrestRestException("private", null, null, "23503", response)
                assertIs<SaveListingFailure.ListingUnavailable>(
                    assertIs<Result.Error>(f.adapter.save("viewer", actionListingId)).exception
                )
            } finally {
                client.close()
            }
        }
    }

    @Test
    fun thrownUnknownFailuresAreTranslatedAndCancellationPropagates() = runTest {
        val f = HomeSaveFixture().apply { thrown = IllegalStateException("raw backend error") }
        assertIs<SaveListingFailure.Unknown>(assertIs<Result.Error>(f.adapter.save("viewer", actionListingId)).exception)
        f.thrown = CancellationException()
        assertFailsWith<CancellationException> { f.adapter.save("viewer", actionListingId) }
        f.thrown = null
        f.failure = CancellationException()
        assertFailsWith<CancellationException> { f.adapter.save("viewer", actionListingId) }
    }

    @Test
    fun blankUserDoesNotCallMarketplace() = runTest {
        val f = HomeSaveFixture()
        assertIs<SaveListingFailure.NotSignedIn>(assertIs<Result.Error>(f.adapter.save(" ", actionListingId)).exception)
        assertEquals(emptyList(), f.calls)
    }
}

private class HomeSaveFixture {
    var failure: Throwable? = null
    var thrown: Throwable? = null
    val calls = mutableListOf<Pair<String, MarketplaceItemId>>()
    private val repository = object : MarketplaceRepository by InMemoryMarketplaceRepository() {
        override suspend fun bookmarkListing(userId: String, itemId: MarketplaceItemId): Result<Unit> {
            calls += userId to itemId
            thrown?.let { throw it }
            return failure?.let { Result.Error(it) } ?: Result.Success(Unit)
        }
    }
    val adapter = MarketplaceHomeSaveAdapter(BookmarkMarketplaceListing(repository))
}
