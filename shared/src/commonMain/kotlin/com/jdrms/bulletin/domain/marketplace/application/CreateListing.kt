package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.repository.ListingRepository
import com.jdrms.bulletin.domain.marketplace.domain.service.ListingPolicy

class CreateListing(
    private val listingRepository: ListingRepository,
    private val listingPolicy: ListingPolicy = ListingPolicy()
) {
    suspend operator fun invoke(listing: Listing): Result<Listing> {
        val validation = listingPolicy.validateListing(listing)
        if (validation.isError()) {
            return Result.Error((validation as Result.Error).exception)
        }
        return listingRepository.createListing(listing)
    }
}
