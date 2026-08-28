package com.jdrms.bulletin.domain.marketplace.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.Category
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingId
import com.jdrms.bulletin.domain.profile.domain.model.UserId

interface ListingRepository {
    suspend fun getListings(): List<Listing>
    suspend fun getListingById(id: ListingId): Listing?
    suspend fun searchListings(query: String, category: Category?): List<Listing>
    suspend fun createListing(listing: Listing): Result<Listing>
    suspend fun updateListing(listing: Listing): Result<Listing>
    suspend fun deleteListing(id: ListingId): Result<Unit>
    suspend fun toggleFavorite(userId: UserId, listingId: ListingId): Result<Boolean>
    suspend fun getFavoriteListingIds(userId: UserId): List<ListingId>
}
