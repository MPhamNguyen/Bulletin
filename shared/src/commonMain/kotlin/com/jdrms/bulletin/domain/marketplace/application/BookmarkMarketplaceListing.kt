package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.MarketplaceItemId
import com.jdrms.bulletin.domain.marketplace.domain.model.OwnListingBookmarkException
import com.jdrms.bulletin.domain.marketplace.domain.repository.MarketplaceRepository

class BookmarkMarketplaceListing(
    private val repository: MarketplaceRepository
) {
    suspend operator fun invoke(userId: String, itemId: MarketplaceItemId): Result<Unit> {
        if (repository.getItem(itemId)?.sellerId == userId) {
            return Result.Error(OwnListingBookmarkException())
        }
        return repository.bookmarkListing(userId, itemId)
    }
}
