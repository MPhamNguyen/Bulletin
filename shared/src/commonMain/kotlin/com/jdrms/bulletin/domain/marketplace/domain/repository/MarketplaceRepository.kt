package com.jdrms.bulletin.domain.marketplace.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.BookmarkedListingsPage
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingBookmarksPageCursor
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCategory
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceCondition
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItem
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceReview

interface MarketplaceRepository {
    suspend fun getCatalog(): List<MarketplaceItem>
    suspend fun search(
        query: String,
        category: MarketplaceCategory?,
        condition: MarketplaceCondition? = null
    ): List<MarketplaceItem>
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
    suspend fun hasReviewForListing(listingId: MarketplaceItemId, reviewerId: String): Boolean = false
    suspend fun submitReview(review: MarketplaceReview): Result<Unit> =
        Result.Error(UnsupportedOperationException("Review submission is unavailable."))
}
