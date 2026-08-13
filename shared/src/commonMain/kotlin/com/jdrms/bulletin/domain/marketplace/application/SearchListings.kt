package com.jdrms.bulletin.domain.marketplace.application

import com.jdrms.bulletin.domain.marketplace.domain.model.Category
import com.jdrms.bulletin.domain.marketplace.domain.model.Listing
import com.jdrms.bulletin.domain.marketplace.domain.model.ListingId
import com.jdrms.bulletin.domain.marketplace.domain.repository.ListingRepository

class SearchListings(
    private val listingRepository: ListingRepository
) {
    suspend fun getListings(): List<Listing> {
        return listingRepository.getListings()
    }

    suspend fun search(query: String, category: Category?): List<Listing> {
        return listingRepository.searchListings(query, category)
    }

    suspend fun getById(id: ListingId): Listing? {
        return listingRepository.getListingById(id)
    }
}
