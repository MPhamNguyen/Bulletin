package com.jdrms.bulletin.domain.listings.domain.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.SellerId

interface ListingsRepository {
    suspend fun createListing(listing: Listing): Result<Listing>
    suspend fun updateListing(listing: Listing): Result<Listing>
    suspend fun getListing(id: ListingId): Listing?
    suspend fun deleteListing(id: ListingId, sellerId: SellerId): Result<Unit>
    suspend fun getSellerListings(sellerId: SellerId): List<Listing>
    suspend fun getAllListings(): List<Listing>
}
