package com.jdrms.bulletin.domain.listings.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import com.jdrms.bulletin.domain.listings.domain.service.ListingValidationPolicy

class CreateListing(
    private val listingsRepository: ListingsRepository,
    private val policy: ListingValidationPolicy = ListingValidationPolicy()
) {
    suspend operator fun invoke(listing: Listing): Result<Listing> {
        val validation = policy.validateListing(listing)
        if (validation.isError()) {
            return Result.Error((validation as Result.Error).exception)
        }
        return listingsRepository.createListing(listing)
    }
}

class ManageListing(
    private val listingsRepository: ListingsRepository
) {
    suspend fun updateListing(listing: Listing): Result<Listing> {
        return listingsRepository.updateListing(listing)
    }

    suspend fun deleteListing(id: ListingId): Result<Unit> {
        return listingsRepository.deleteListing(id)
    }
}

class GetSellerListings(
    private val listingsRepository: ListingsRepository
) {
    suspend operator fun invoke(sellerId: SellerId): List<Listing> {
        return listingsRepository.getSellerListings(sellerId)
    }

    suspend fun getAll(): List<Listing> {
        return listingsRepository.getAllListings()
    }
}
