package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.identity.domain.model.UserId
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingId
import com.jdrms.bulletin.domain.marketplace.domain.repository.ListingRepository

class ManageListing(
    private val listingRepository: ListingRepository
) {
    suspend fun updateListing(listing: Listing): Result<Listing> {
        return listingRepository.updateListing(listing)
    }

    suspend fun deleteListing(id: ListingId): Result<Unit> {
        return listingRepository.deleteListing(id)
    }
}

class ToggleFavorite(
    private val listingRepository: ListingRepository
) {
    suspend operator fun invoke(userId: UserId, listingId: ListingId): Result<Boolean> {
        return listingRepository.toggleFavorite(userId, listingId)
    }

    suspend fun getFavorites(userId: UserId): List<ListingId> {
        return listingRepository.getFavoriteListingIds(userId)
    }
}
