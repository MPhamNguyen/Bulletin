package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.ListingBookmarkDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.ListingBookmarkWithListingDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.SupabaseMarketplaceListingDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.SupabaseMarketplaceProfileDto
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class SupabaseListingBookmarksTest {
    @Test
    fun bookmarkMembershipIsCachedUntilItsTtlExpires() = runTest {
        var nowMillis = 0L
        var fetchCount = 0
        val store = SupabaseListingBookmarks(
            fetch = { userId, _ ->
                fetchCount += 1
                listOf(ListingBookmarkDto(userId, "listing-one"))
            },
            upsert = {},
            delete = { _, _ -> },
            fetchPage = { _, _, _ -> emptyList() },
            cachePolicy = BookmarkCachePolicy(ttlMillis = 100L) { nowMillis }
        )

        store.get("user-one")
        store.get("user-one")
        assertEquals(1, fetchCount)

        nowMillis = 100L
        store.get("user-one")
        assertEquals(2, fetchCount)
    }

    @Test
    fun successfulMutationsKeepCachedMembershipConsistent() = runTest {
        val rows = mutableSetOf(ListingBookmarkDto("user-one", "listing-one"))
        var fetchCount = 0
        val store = SupabaseListingBookmarks(
            fetch = { userId, _ ->
                fetchCount += 1
                rows.filter { it.userId == userId }
            },
            upsert = { rows.add(it) },
            delete = { userId, listingId -> rows.remove(ListingBookmarkDto(userId, listingId)) },
            fetchPage = { _, _, _ -> emptyList() }
        )
        val firstId = MarketplaceItemId("listing:listing-one")
        val secondId = MarketplaceItemId("listing:listing-two")

        store.get("user-one")
        store.bookmark("user-one", secondId)
        assertEquals(setOf(firstId, secondId), store.get("user-one").ids)

        store.remove("user-one", firstId)
        assertEquals(setOf(secondId), store.get("user-one").ids)
        assertEquals(1, fetchCount)
    }

    @Test
    fun failedMutationLeavesCachedMembershipUnchanged() = runTest {
        var fetchCount = 0
        val initialId = MarketplaceItemId("listing:listing-one")
        val store = SupabaseListingBookmarks(
            fetch = { userId, _ ->
                fetchCount += 1
                listOf(ListingBookmarkDto(userId, "listing-one"))
            },
            upsert = { error("Write failed") },
            delete = { _, _ -> },
            fetchPage = { _, _, _ -> emptyList() }
        )

        store.get("user-one")
        assertFailsWith<IllegalStateException> {
            store.bookmark("user-one", MarketplaceItemId("listing:listing-two"))
        }

        assertEquals(setOf(initialId), store.get("user-one").ids)
        assertEquals(1, fetchCount)
    }

    @Test
    fun bookmarkMutationReturnsErrorWhenTimeoutExpires() = runTest {
        val result = runBookmarkMutation(timeoutMillis = 1L) { delay(100L) }

        assertTrue(result.isError())
        assertEquals("Bookmark request timed out.", result.exceptionOrNull()?.message)
    }

    @Test
    fun bookmarkMutationPreservesFailuresAndCancellation() = runTest {
        val failure = IllegalStateException("Write failed")
        val result = runBookmarkMutation { throw failure }

        assertEquals(failure, result.exceptionOrNull())
        assertFailsWith<CancellationException> {
            runBookmarkMutation { throw CancellationException("Cancelled") }
        }
    }

    @Test
    fun bookmarkCachePolicyRejectsNonPositiveTtl() {
        assertFailsWith<IllegalArgumentException> { BookmarkCachePolicy(ttlMillis = 0L) }
    }

    @Test
    fun fetchesEveryPageAndReconstructsBookmarksFromPersistedRows() = runTest {
        val rows = (1..501).map { ListingBookmarkDto("user-one", "listing-$it") }
        val offsets = mutableListOf<Long>()
        val store = SupabaseListingBookmarks(
            fetch = { userId, offset ->
                offsets += offset
                rows.filter { it.userId == userId }.drop(offset.toInt()).take(500)
            },
            upsert = {},
            delete = { _, _ -> },
            fetchPage = { _, _, _ -> emptyList() }
        )

        assertEquals(501, store.get("user-one").ids.size)
        assertEquals(listOf(0L, 500L), offsets)
    }

    @Test
    fun bookmarkingIsIdempotentAndNormalizesListingId() = runTest {
        val rows = mutableSetOf<ListingBookmarkDto>()
        val store = SupabaseListingBookmarks(
            fetch = { userId, offset -> rows.filter { it.userId == userId }.drop(offset.toInt()) },
            upsert = { rows.add(it) },
            delete = { userId, listingId -> rows.remove(ListingBookmarkDto(userId, listingId)) },
            fetchPage = { _, _, _ -> emptyList() }
        )
        val id = MarketplaceItemId("listing:listing-one")

        store.bookmark("user-one", id)
        store.bookmark("user-one", id)
        assertEquals(setOf(ListingBookmarkDto("user-one", "listing-one")), rows)
        assertEquals(setOf(id), store.get("user-one").ids)
        assertEquals(emptySet(), store.get("user-two").ids)
    }

    @Test
    fun removingDeletesOnlyTheRequestedUsersNormalizedBookmark() = runTest {
        val rows = mutableSetOf(
            ListingBookmarkDto("user-one", "listing-one"),
            ListingBookmarkDto("user-two", "listing-one")
        )
        val store = SupabaseListingBookmarks(
            fetch = { userId, offset -> rows.filter { it.userId == userId }.drop(offset.toInt()) },
            upsert = { rows.add(it) },
            delete = { userId, listingId -> rows.remove(ListingBookmarkDto(userId, listingId)) },
            fetchPage = { _, _, _ -> emptyList() }
        )

        store.remove("user-one", MarketplaceItemId("listing:listing-one"))

        assertEquals(setOf(ListingBookmarkDto("user-two", "listing-one")), rows)
    }

    @Test
    fun pageFetchReturnsCanonicalIdsAndMappedListingsWithLookaheadCursor() = runTest {
        var nowMillis = 0L
        var fetchPageCount = 0
        var requestedOffset = -1L
        var requestedPageSize = -1
        val store = SupabaseListingBookmarks(
            fetch = { _, _ -> emptyList() },
            upsert = {},
            delete = { _, _ -> },
            fetchPage = { _, offset, pageSize ->
                fetchPageCount += 1
                requestedOffset = offset
                requestedPageSize = pageSize
                bookmarkPageRows()
            },
            cachePolicy = BookmarkCachePolicy(ttlMillis = 100L) { nowMillis }
        )

        val page = store.getPage("user-one", null, 2)
        store.getPage("user-one", null, 2)

        assertEquals(0L, requestedOffset)
        assertEquals(2, requestedPageSize)
        assertEquals(1, fetchPageCount)
        assertEquals(listOf("listing:one", "listing:two"), page.listingIds.map { it.value })
        assertEquals(listOf("listing:one", "listing:two"), page.listings.map { it.id.value })
        assertEquals(listOf("Seller one", "Seller two"), page.listings.map { it.sellerName })
        assertEquals(2, page.nextCursor?.offset)

        nowMillis = 100L
        store.getPage("user-one", null, 2)
        assertEquals(2, fetchPageCount)

        store.bookmark("user-one", MarketplaceItemId("listing:new"))
        store.getPage("user-one", null, 2)
        assertEquals(3, fetchPageCount)
    }
}

private fun Result<Unit>.exceptionOrNull(): Throwable? = (this as? Result.Error)?.exception

private fun bookmarkPageRows(): List<ListingBookmarkWithListingDto> =
    listOf("one", "two", "three").map { id ->
        ListingBookmarkWithListingDto(
            listingId = id,
            listing = SupabaseMarketplaceListingDto(
                id = id,
                name = "Listing $id",
                userId = "seller-$id",
                price = 10.0,
                description = "Description for $id",
                profile = SupabaseMarketplaceProfileDto("Seller $id")
            )
        )
    }
