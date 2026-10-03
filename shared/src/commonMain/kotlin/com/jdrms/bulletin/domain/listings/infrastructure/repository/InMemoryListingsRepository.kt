package com.jdrms.bulletin.domain.listings.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.domain.listings.domain.model.Listing
import com.jdrms.bulletin.domain.listings.domain.model.ListingId
import com.jdrms.bulletin.domain.listings.domain.model.SellerId
import com.jdrms.bulletin.domain.listings.domain.repository.ListingsRepository
import com.jdrms.bulletin.domain.listings.infrastructure.dto.ListingDto
import com.jdrms.bulletin.domain.listings.infrastructure.mapper.ListingMapper

class InMemoryListingsRepository(
    initialListings: List<ListingDto> = defaultSeedListings
) : ListingsRepository {

    private val listings = initialListings.map { ListingMapper.toDomain(it) }.toMutableList()

    override suspend fun createListing(listing: Listing): Result<Listing> {
        listings.add(0, listing)
        return Result.Success(listing)
    }

    override suspend fun updateListing(listing: Listing): Result<Listing> {
        val index = listings.indexOfFirst { it.id == listing.id }
        if (index != -1) {
            listings[index] = listing
            return Result.Success(listing)
        }
        return Result.Error(NoSuchElementException("Listing not found with ID: ${listing.id.value}"))
    }

    override suspend fun getListing(id: ListingId): Listing? = listings.firstOrNull { it.id == id }

    override suspend fun deleteListing(id: ListingId, sellerId: SellerId): Result<Unit> {
        val listing = listings.firstOrNull { it.id == id }
            ?: return Result.Error(NoSuchElementException("Listing not found with ID: ${id.value}"))
        if (!listing.isOwnedBy(sellerId)) {
            return Result.Error(IllegalAccessException("Only the owner can delete this listing."))
        }
        val removed = listings.remove(listing)
        return if (removed) {
            Result.Success(Unit)
        } else {
            Result.Error(NoSuchElementException("Listing not found with ID: ${id.value}"))
        }
    }

    override suspend fun getSellerListings(sellerId: SellerId): List<Listing> {
        return listings.filter { it.sellerId == sellerId }
    }

    override suspend fun getAllListings(): List<Listing> {
        return listings.toList()
    }

    companion object {
        private val defaultSeedListings = listOf(
            ListingDto(
                id = "list_1",
                sellerId = "current_student",
                sellerName = "Dominic Alfonso",
                title = "CECS 328 Algorithms Textbook",
                description = "Hardcover edition, very clean with practice problems marked.",
                price = 40.0,
                category = "TEXTBOOKS",
                condition = "LIKE_NEW",
                status = "AVAILABLE"
            ),
            ListingDto(
                id = "list_2",
                sellerId = "current_student",
                sellerName = "Dominic Alfonso",
                title = "Logitech MX Master 3S Mouse",
                description = "Quiet clicks, ergonomic design, Bluetooth connection.",
                price = 55.0,
                category = "ELECTRONICS",
                condition = "GOOD",
                status = "AVAILABLE"
            )
        )
    }
}
