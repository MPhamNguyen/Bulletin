package com.jdrms.bulletin.domain.marketplace.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.BookmarkedListingsPage
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingBookmarksPageCursor
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId

interface MarketplaceRepository {
    suspend fun getCatalog(): List<MarketplaceItem>
    suspend fun search(query: String, category: MarketplaceCategory?): List<MarketplaceItem>
    suspend fun getItem(id: MarketplaceItemId): MarketplaceItem?
    suspend fun viewListing(listingID: String): Result<Listing>
    suspend fun bookmarkListing(userId: String, itemId: MarketplaceItemId): Result<Unit>
    suspend fun removeListingBookmark(userId: String, itemId: MarketplaceItemId): Result<Unit>
    suspend fun getBookmarkedItemIds(userId: String): Set<MarketplaceItemId>
    suspend fun getBookmarkedListingsPage(
        userId: String,
        cursor: ListingBookmarksPageCursor?,
        pageSize: Int
    ): BookmarkedListingsPage
}
