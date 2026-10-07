package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.ListingBookmarkDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.ListingBookmarkWithListingDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.SupabaseMarketplaceListingDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.SupabaseMarketplaceProfileDto
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SupabaseListingBookmarksTest {
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
        var requestedOffset = -1L
        var requestedPageSize = -1
        val store = SupabaseListingBookmarks(
            fetch = { _, _ -> emptyList() },
            upsert = {},
            delete = { _, _ -> },
            fetchPage = { _, offset, pageSize ->
                requestedOffset = offset
                requestedPageSize = pageSize
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
            }
        )

        val page = store.getPage("user-one", null, 2)

        assertEquals(0L, requestedOffset)
        assertEquals(2, requestedPageSize)
        assertEquals(listOf("listing:one", "listing:two"), page.listingIds.map { it.value })
        assertEquals(listOf("listing:one", "listing:two"), page.listings.map { it.id.value })
        assertEquals(listOf("Seller one", "Seller two"), page.listings.map { it.sellerName })
        assertEquals(2, page.nextCursor?.offset)
    }
}
