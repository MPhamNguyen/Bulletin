package com.jdrms.bulletin.domain.marketplace.infrastructure.repository

import com.jdrms.bulletin.core.common.Result
import com.jdrms.bulletin.core.network.SupabaseConfig
import com.jdrms.bulletin.domain.identity.domain.model.UserId
import com.jdrms.bulletin.domain.marketplace.domain.model.*
import com.jdrms.bulletin.domain.marketplace.domain.repository.ListingRepository
import com.jdrms.bulletin.domain.marketplace.infrastructure.dto.ListingDto
import com.jdrms.bulletin.domain.marketplace.infrastructure.mapper.ListingMapper

class SupabaseListingRepository(
    private val supabaseConfig: SupabaseConfig = SupabaseConfig()
) : ListingRepository {

    private val listingsMap = mutableMapOf<String, ListingDto>(
        "item_1" to ListingDto(
            id = "item_1",
            sellerId = "user_101",
            sellerName = "Dominic Alfonso",
            title = "CECS 491 Software Engineering Textbook",
            description = "Like new textbook for CECS 491A/B. Clean pages.",
            priceAmount = 45.0,
            categoryName = "TEXTBOOKS",
            statusCode = "AVAILABLE",
            createdAtMillis = 1723500000000L
        ),
        "item_2" to ListingDto(
            id = "item_2",
            sellerId = "user_102",
            sellerName = "Sean Gallagher",
            title = "Apple iPad Air 64GB - Space Gray",
            description = "Great condition, includes Apple Pencil 2nd gen.",
            priceAmount = 320.0,
            categoryName = "ELECTRONICS",
            statusCode = "AVAILABLE",
            createdAtMillis = 1723510000000L
        ),
        "item_3" to ListingDto(
            id = "item_3",
            sellerId = "user_103",
            sellerName = "Minh Pham-Nguyen",
            title = "Ergonomic Desk Chair",
            description = "Mesh back desk chair, perfect for dorm or apartment.",
            priceAmount = 60.0,
            categoryName = "FURNITURE",
            statusCode = "AVAILABLE",
            createdAtMillis = 1723520000000L
        )
    )

    private val userFavorites = mutableMapOf<String, MutableSet<String>>()

    override suspend fun getListings(): List<Listing> {
        return listingsMap.values.map { ListingMapper.toDomain(it) }
    }

    override suspend fun getListingById(id: ListingId): Listing? {
        return listingsMap[id.value]?.let { ListingMapper.toDomain(it) }
    }

    override suspend fun searchListings(query: String, category: Category?): List<Listing> {
        return listingsMap.values
            .map { ListingMapper.toDomain(it) }
            .filter { listing ->
                val matchesQuery = query.isBlank() ||
                        listing.title.contains(query, ignoreCase = true) ||
                        listing.description.contains(query, ignoreCase = true)
                val matchesCategory = category == null || listing.category == category
                matchesQuery && matchesCategory
            }
    }

    override suspend fun createListing(listing: Listing): Result<Listing> {
        val dto = ListingMapper.toDto(listing)
        listingsMap[dto.id] = dto
        return Result.Success(listing)
    }

    override suspend fun updateListing(listing: Listing): Result<Listing> {
        val dto = ListingMapper.toDto(listing)
        listingsMap[dto.id] = dto
        return Result.Success(listing)
    }

    override suspend fun deleteListing(id: ListingId): Result<Unit> {
        listingsMap.remove(id.value)
        return Result.Success(Unit)
    }

    override suspend fun toggleFavorite(userId: UserId, listingId: ListingId): Result<Boolean> {
        val set = userFavorites.getOrPut(userId.value) { mutableSetOf() }
        val isFavoriteNow = if (set.contains(listingId.value)) {
            set.remove(listingId.value)
            false
        } else {
            set.add(listingId.value)
            true
        }
        return Result.Success(isFavoriteNow)
    }

    override suspend fun getFavoriteListingIds(userId: UserId): List<ListingId> {
        return userFavorites[userId.value]?.map { ListingId(it) } ?: emptyList()
    }
}
