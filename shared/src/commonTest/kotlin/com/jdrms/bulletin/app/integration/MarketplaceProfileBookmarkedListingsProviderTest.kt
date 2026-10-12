package com.jdrms.bulletin.app.integration

import com.jdrms.bulletin.domain.listings.infrastructure.repository.InMemoryListingsRepository
import com.jdrms.bulletin.domain.marketplace.application.BookmarkMarketplaceListing
import com.jdrms.bulletin.domain.marketplace.application.GetMarketplaceListingBookmarks
import com.jdrms.bulletin.domain.marketplace.application.RemoveMarketplaceListingBookmark
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import com.jdrms.bulletin.domain.profile.domain.model.UserId
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class MarketplaceProfileBookmarkedListingsProviderTest {
    @Test
    fun returnsOnlyListingsBookmarkedByRequestedUser() = runTest {
        val repository = InMemoryMarketplaceRepository()
        val bookmarkListing = BookmarkMarketplaceListing(repository)
        bookmarkListing("current_student", MarketplaceItemId("mkt_1"))
        bookmarkListing("another_student", MarketplaceItemId("mkt_2"))

        val provider = MarketplaceProfileBookmarkedListingsProvider(
            GetMarketplaceListingBookmarks(repository),
            RemoveMarketplaceListingBookmark(repository)
        )

        val listings = provider.getBookmarkedListings(UserId("current_student"), null, 20).listings

        assertEquals(listOf("mkt_1"), listings.map { it.id })
        assertEquals("Great condition, minimal highlighting. Required for MATH 122/123.", listings.single().description)
        assertEquals("GOOD", listings.single().condition)
    }

    @Test
    fun includesListingsFromTheListingsContext() = runTest {
        val marketplace = InMemoryMarketplaceRepository()
        val bookmarks = BookmarkMarketplaceListing(marketplace)
        bookmarks("current_student", MarketplaceItemId("listing:list_1"))
        val provider = MarketplaceProfileBookmarkedListingsProvider(
            GetMarketplaceListingBookmarks(marketplace),
            RemoveMarketplaceListingBookmark(marketplace),
            InMemoryListingsRepository()
        )

        val listings = provider.getBookmarkedListings(UserId("current_student"), null, 20).listings

        assertEquals(listOf("listing:list_1"), listings.map { it.id })
        assertEquals("CECS 328 Algorithms Textbook", listings.single().title)
        assertEquals("LIKE_NEW", listings.single().condition)
    }

    @Test
    fun removesBookmarkAndRefreshQueryNoLongerReturnsListing() = runTest {
        val repository = InMemoryMarketplaceRepository()
        val bookmarkListing = BookmarkMarketplaceListing(repository)
        val listingId = MarketplaceItemId("mkt_1")
        bookmarkListing("current_student", listingId)
        val provider = MarketplaceProfileBookmarkedListingsProvider(
            GetMarketplaceListingBookmarks(repository),
            RemoveMarketplaceListingBookmark(repository)
        )

        provider.removeBookmark(UserId("current_student"), listingId.value)

        assertEquals(
            emptyList(),
            provider.getBookmarkedListings(UserId("current_student"), null, 20).listings
        )
    }

    @Test
    fun returnsBookmarksInPagesWithoutLoadingEachListingIndividually() = runTest {
        val repository = InMemoryMarketplaceRepository()
        val bookmarkListing = BookmarkMarketplaceListing(repository)
        bookmarkListing("current_student", MarketplaceItemId("mkt_1"))
        bookmarkListing("current_student", MarketplaceItemId("mkt_2"))
        val provider = MarketplaceProfileBookmarkedListingsProvider(
            GetMarketplaceListingBookmarks(repository),
            RemoveMarketplaceListingBookmark(repository)
        )

        val firstPage = provider.getBookmarkedListings(UserId("current_student"), null, 1)
        val secondPage = provider.getBookmarkedListings(
            UserId("current_student"),
            firstPage.nextCursor,
            1
        )

        assertEquals(listOf("mkt_1"), firstPage.listings.map { it.id })
        assertEquals(listOf("mkt_2"), secondPage.listings.map { it.id })
        assertEquals(null, secondPage.nextCursor)
    }
}
