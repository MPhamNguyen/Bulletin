package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.OwnListingBookmarkException
import com.jdrms.bulletin.domain.marketplace.infrastructure.repository.InMemoryMarketplaceRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class BookmarkMarketplaceListingTest {
    @Test
    fun sellerCannotBookmarkOwnListing() = runTest {
        val repository = InMemoryMarketplaceRepository()
        val listingId = MarketplaceItemId("mkt_1")

        val result = BookmarkMarketplaceListing(repository)("seller_101", listingId)

        assertIs<OwnListingBookmarkException>(assertIs<Result.Error>(result).exception)
        assertTrue(repository.getBookmarkedItemIds("seller_101").isEmpty())
    }

    @Test
    fun anotherUserCanBookmarkListing() = runTest {
        val repository = InMemoryMarketplaceRepository()
        val listingId = MarketplaceItemId("mkt_1")

        val result = BookmarkMarketplaceListing(repository)("buyer_201", listingId)

        assertIs<Result.Success<Unit>>(result)
        assertEquals(setOf(listingId), repository.getBookmarkedItemIds("buyer_201"))
    }
}
